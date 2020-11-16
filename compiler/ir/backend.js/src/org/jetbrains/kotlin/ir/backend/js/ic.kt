/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.lower.generateTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult

// TODO test purpose only
// klib path -> ic data path
val icCache = mutableMapOf<String, SerializedIcData>()

fun prepareIcCaches(
    project: Project,
    analyzer: AbstractAnalyzerWithCompilerReport,
    configuration: CompilerConfiguration,
    allDependencies: KotlinLibraryResolveResult,
) {
    val irFactory = PersistentIrFactory()

    // only process stdlib for now
    val stdlibResolved = findStdlib(allDependencies)
    val stdlibKlib = stdlibResolved.getFullList().single()

    icCache.getOrPut(stdlibKlib.libraryName) {
        val mainModule = MainModule.Klib(stdlibKlib)

        val (moduleFragment: IrModuleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
            loadIr(project, mainModule, analyzer, configuration, stdlibResolved, emptyList(), irFactory)

        val moduleDescriptor = moduleFragment.descriptor

        val context = JsIrBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, emptySet(), configuration, irFactory)

        // Load declarations referenced during `context` initialization
        val irProviders = listOf(deserializer)
        ExternalDependenciesGenerator(symbolTable, irProviders, configuration.languageVersionSettings).generateUnboundSymbolsAsDependencies()

        deserializer.postProcess()
        symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

        // This won't work incrementally
        moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)

        // TODO should be done incrementally
        generateTests(context, moduleFragment)

        lowerPreservingIcData(listOf(moduleFragment), irFactory, context)

        irFactory.allDeclarations.forEach {
            // filter newly created
            // group by file
            // group by stage
            //    serialize declaration
            //    serialize Carriers
            //    serialize mappings
        }

        SerializedIcData()
    }
}

private fun findStdlib(allDependencies: KotlinLibraryResolveResult): KotlinLibraryResolveResult {
    var result: KotlinLibraryResolveResult? = null

    allDependencies.forEach { klib, _ ->
        val resolvedLib = allDependencies.filterRoots {
            it.library == klib
        }

        if (resolvedLib.getFullList().size == 1) result = resolvedLib
    }

    return result!!
}

fun loadIrForIc(stdlibIcCachePath: String) {

}

fun serializeIrForIc(stdlibIcCachePath: String) {

}

class SerializedIcData()