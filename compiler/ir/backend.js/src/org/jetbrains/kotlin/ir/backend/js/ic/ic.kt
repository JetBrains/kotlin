/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.generateTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.allUnbound
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import java.io.File

// TODO test purpose only
// klib path -> ic data
val icCache = mutableMapOf<String, SerializedIcData>()

// TODO change API to support not only stdlib
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

        val (moduleFragment: IrModuleFragment, _, irBuiltIns, symbolTable, deserializer) =
            loadIr(project, mainModule, analyzer, configuration, stdlibResolved, emptyList(), irFactory)

        val moduleDescriptor = moduleFragment.descriptor

        val context = JsIrBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, emptySet(), configuration, irFactory = irFactory)

        // Load declarations referenced during `context` initialization
        val irProviders = listOf(deserializer)
        ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

        deserializer.postProcess()
        symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

        // This won't work incrementally
        // TODO don't move
        moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)

        // TODO should be done incrementally
        generateTests(context, moduleFragment)

        lowerPreservingIcData(listOf(moduleFragment), irFactory, context)

        IcSerializer(
            context.irBuiltIns,
            context.mapping,
            irFactory,
            deserializer,
            moduleFragment
        ).serializeDeclarations(irFactory.allDeclarations)
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

fun loadIrForIc(
    linker: JsIrLinker,
    module: IrModuleFragment,
    context: JsIrBackendContext,
    checkEq: (String, String) -> Unit,
) {
    val icData = icCache.values.single() // TODO find a stable key present both in klib and module

    IcDeserializer(linker, context).injectIcData(module, icData)

    linker.symbolTable.noUnboundLeft("Unbound symbols found")

    for (file in module.files) {
        println(file.path)

        val actual = file.dumpKotlinLike()

        checkEq("/home/ab/tmp/simple-dump/${sanitizeName(file.path)}", actual)
    }

//    TODO("=================== loaded!")
}