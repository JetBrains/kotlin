/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.backend.wasm.MultimoduleCompileOptions
import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.WasmModuleDependencyImport
import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragmentsMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactMultimodule
import org.jetbrains.kotlin.backend.wasm.ir2wasm.ModuleReferencedDeclarations
import org.jetbrains.kotlin.backend.wasm.ir2wasm.ModuleReferencedTypes
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.hasDeclarations
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.utils.addIfNotNull

private fun List<WasmIrProgramFragmentsMultimodule>.collectTypeReferences(builtInTypes: ModuleReferencedTypes): ModuleReferencedTypes {
    val currentDeclarationTypes = ModuleReferencedTypes()
    forEach { fragment ->
        currentDeclarationTypes.gcTypes.addAll(fragment.referencedTypes.gcTypes)
        currentDeclarationTypes.functionTypes.addAll(fragment.referencedTypes.functionTypes)
    }
    currentDeclarationTypes.gcTypes.addAll(builtInTypes.gcTypes)
    currentDeclarationTypes.functionTypes.addAll(builtInTypes.functionTypes)

    return currentDeclarationTypes
}

private fun List<WasmIrProgramFragmentsMultimodule>.collectDeclarationReferences(builtInFunctions: Set<IdSignature>): ModuleReferencedDeclarations {
    val currentDeclarationReferences = ModuleReferencedDeclarations()
    forEach { fragment ->
        currentDeclarationReferences.functions.addAll(fragment.referencedDeclarations.functions)
        currentDeclarationReferences.globalVTable.addAll(fragment.referencedDeclarations.globalVTable)
        currentDeclarationReferences.globalClassITable.addAll(fragment.referencedDeclarations.globalClassITable)
        currentDeclarationReferences.rttiGlobal.addAll(fragment.referencedDeclarations.rttiGlobal)
    }
    currentDeclarationReferences.functions.addAll(builtInFunctions)

    return currentDeclarationReferences
}

private fun List<WasmIrProgramFragmentsMultimodule>.collectStdlibBuiltins(): Pair<ModuleReferencedTypes, Set<IdSignature>> {
    val referencedTypes = ModuleReferencedTypes()
    val referencedDeclarations = mutableSetOf<IdSignature>()
    forEach { fragment ->
        fragment.mainFragment.definedDeclarations.builtinIdSignatures?.let {
            referencedTypes.gcTypes.addAll(fragment.referencedTypes.gcTypes)
            referencedTypes.functionTypes.addAll(fragment.referencedTypes.functionTypes)
            referencedDeclarations.addIfNotNull(it.registerModuleDescriptor)
            referencedDeclarations.addIfNotNull(it.createString)
            referencedDeclarations.addIfNotNull(it.tryGetAssociatedObject)
            referencedDeclarations.addIfNotNull(it.runRootSuites)
            referencedDeclarations.addIfNotNull(it.jsToKotlinStringAdapter)
        }
    }
    return referencedTypes to referencedDeclarations
}

fun compileIncrementally(
    artifacts: List<WasmModuleArtifactMultimodule>,
    configuration: CompilerConfiguration
): List<WasmIrModuleConfiguration> {

//    val moduleToLoadedFragments = artifacts.map { module ->
//        module to module.fileArtifacts.mapNotNull { it.loadIrFragments() }
//    }

    val stdLibArtifact = artifacts.first { it.moduleName == "<kotlin>" }

    val stdlibLoadedFragments by lazy {
        stdLibArtifact.fileArtifacts.mapNotNull { it.loadIrFragments() }
    }

    val builtInsReferences by lazy { stdlibLoadedFragments.collectStdlibBuiltins() }

    val parameters = artifacts.mapNotNull { currentArtifact ->
        if (currentArtifact.fileArtifacts.none { it.isModified() }) return@mapNotNull null

        if (currentArtifact == stdLibArtifact) {
            val multimoduleOptions = MultimoduleCompileOptions(
                stdlibModuleNameForImport = null,
                dependencyModules = emptySet(),
                initializeUnit = true,
            )
            return@mapNotNull WasmIrModuleConfiguration(
                wasmCompiledFileFragments = stdlibLoadedFragments.map { it.mainFragment },
                moduleName = currentArtifact.moduleName,
                configuration = configuration,
                typeScriptFragment = null,
                baseFileName = currentArtifact.externalModuleName ?: encodeModuleName(currentArtifact.moduleName),
                multimoduleOptions = multimoduleOptions,
            )
        }

        val currentIcFragments = currentArtifact.fileArtifacts.mapNotNull { it.loadIrFragments() }

        val currentModuleFragments = mutableListOf<WasmCompiledFileFragment>()
        currentIcFragments.forEach { currentModuleFragments.add(it.mainFragment) }

        val currentModuleImports = mutableSetOf<WasmModuleDependencyImport>()
        val dependencies = artifacts.filterNot { it == currentArtifact }


        dependencies.forEach { dependencyArtifact ->

            dependencyArtifact.fileArtifacts

            dependencyFragments.forEach { fragment ->
                val projection = fragment.dependencyFragment
                    .makeProjection(currentDeclarationTypes, currentDeclarationReferences)

                if (projection.definedDeclarations.hasDeclarations) {
                    val dependencyImport = WasmModuleDependencyImport(
                        name = dependencyModule.moduleName,
                        fileName = dependencyModule.externalModuleName ?: encodeModuleName(dependencyModule.moduleName)
                    )
                    currentModuleImports.add(dependencyImport)
                }

                currentModuleFragments.add(projection)
            }
        }

        val multimoduleOptions = MultimoduleCompileOptions(
            stdlibModuleNameForImport = stdlibModule.moduleName,
            dependencyModules = currentModuleImports,
            initializeUnit = false,
        )

        val currentFileBase = if (currentModule == moduleToLoadedFragments.last().first) {
            configuration.moduleName!!
        } else {
            currentModule.externalModuleName ?: encodeModuleName(currentModule.moduleName)
        }

        WasmIrModuleConfiguration(
            wasmCompiledFileFragments = currentModuleFragments,
            moduleName = currentModule.moduleName,
            configuration = configuration,
            typeScriptFragment = null,
            baseFileName = currentFileBase,
            multimoduleOptions = multimoduleOptions,
        )
    }
}