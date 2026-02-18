/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.MultimoduleCompileOptions
import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.WasmModuleDependencyImport
import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragmentsMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactMultimodule
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import kotlin.collections.mutableSetOf
import kotlin.collections.set

private val WasmModuleArtifactMultimodule.outputFileName: String
    get() = externalModuleName ?: encodeModuleName(moduleName)

private val linkerFunctionSignatures = setOf(
    Synthetics.Functions.registerModuleDescriptorBuiltIn.value,
    Synthetics.Functions.createStringBuiltIn.value,
    Synthetics.Functions.tryGetAssociatedObjectBuiltIn.value,
    Synthetics.Functions.jsToKotlinStringAdapterBuiltIn.value,
    Synthetics.Functions.jsToKotlinAnyAdapterBuiltIn.value,
)
private val linkerTypeSignatures = setOf(
    Synthetics.HeapTypes.associatedObjectGetterWrapper.type,
    Synthetics.HeapTypes.throwableBuiltInType.type,
    Synthetics.HeapTypes.anyBuiltInType.type,
)

private fun WasmCompiledDependencyFileFragment.hasBuiltinSignature() =
    linkerFunctionSignatures.any { it in definedDeclarations.definedFunctions } ||
            linkerTypeSignatures.any { it in definedTypes.definedGcTypes } ||
            linkerTypeSignatures.any { it in definedTypes.definedVTableGcTypes }

private fun WasmModuleArtifactMultimodule.loadDependency(
    builtInStdlibFragments: MutableList<WasmIrProgramFragmentsMultimodule>,
    isStdlibArtifact: Boolean
): List<WasmCompiledDependencyFileFragment> {
    val loadedDependencies = mutableListOf<WasmCompiledDependencyFileFragment>()
    for (fileArtifact in this.fileArtifacts) {
        val loadedFragment = fileArtifact.loadIrDependencyFragments() ?: continue
        loadedDependencies.add(loadedFragment)

        if (isStdlibArtifact && loadedFragment.hasBuiltinSignature()) {
            builtInStdlibFragments.add(fileArtifact.loadIrFragments()!!)
        }
    }
    return loadedDependencies
}

private fun WasmModuleArtifactMultimodule.loadRecompileAndDependency(
    builtInStdlibFragment: MutableList<WasmIrProgramFragmentsMultimodule>,
    isStdlibArtifact: Boolean
): Pair<List<WasmIrProgramFragmentsMultimodule>, List<WasmCompiledDependencyFileFragment>> {
    val loadedFragments = mutableListOf<WasmIrProgramFragmentsMultimodule>()
    val loadedDependencyFragments = mutableListOf<WasmCompiledDependencyFileFragment>()
    for (fileArtifact in this.fileArtifacts) {
        val loadedFragment = fileArtifact.loadIrFragments() ?: continue
        loadedFragments.add(loadedFragment)
        val dependencyFragment = WasmCompiledDependencyFileFragment(
            definedTypes = loadedFragment.definedTypes,
            definedDeclarations = loadedFragment.dependencyDeclarations
        )
        loadedDependencyFragments.add(dependencyFragment)

        if (isStdlibArtifact && dependencyFragment.hasBuiltinSignature()) {
            builtInStdlibFragment.add(loadedFragment)
        }
    }

    return loadedFragments to loadedDependencyFragments
}

private fun compileArtifact(
    artifact: WasmModuleArtifactMultimodule,
    dependencyFragments: Map<WasmModuleArtifactMultimodule, List<WasmCompiledDependencyFileFragment>>,
    loadedFragments: List<WasmIrProgramFragmentsMultimodule>,
    dependencyResolutionMap: Map<String, String>,
    builtInStdlibFragment: MutableList<WasmIrProgramFragmentsMultimodule>,
    configuration: CompilerConfiguration,
    stdlibModuleName: String,
): WasmIrModuleConfiguration {

    val currentCodeFragments = mutableListOf<WasmCompiledFileFragment>()
    loadedFragments.mapTo(currentCodeFragments) { fragment ->
        WasmCompiledCodeFileFragment(fragment.definedTypes, fragment.codeDeclarations, fragment.linkerData)
    }

    val currentModuleTypeReferences = (loadedFragments + builtInStdlibFragment).collectTypeReferences()
    currentModuleTypeReferences.gcTypes.addAll(linkerTypeSignatures)
    val currentModuleDeclarationReferences = (loadedFragments + builtInStdlibFragment).collectDeclarationReferences()
    currentModuleDeclarationReferences.functions.addAll(linkerFunctionSignatures)

    val currentModuleImports = mutableSetOf<WasmModuleDependencyImport>()

    for ((dependencyArtifact, dependencyFragments) in dependencyFragments) {
        if (dependencyArtifact == artifact) continue

        var hasImportsFromDependency = false
        dependencyFragments.forEach { fragment ->
            val projectedTypes = fragment.definedTypes.makeProjection(currentModuleTypeReferences)
            val projectedDeclarations = fragment.definedDeclarations.makeProjection(currentModuleDeclarationReferences)
            val projectedFragment = WasmCompiledDependencyFileFragment(
                definedTypes = projectedTypes,
                definedDeclarations = projectedDeclarations,
            )
            hasImportsFromDependency = hasImportsFromDependency || projectedFragment.definedDeclarations.hasDeclarations
            currentCodeFragments.add(projectedFragment)
        }

        if (hasImportsFromDependency) {
            val dependencyImport = WasmModuleDependencyImport(
                name = dependencyArtifact.moduleName,
                fileName = dependencyResolutionMap[dependencyArtifact.moduleName]
                    ?: dependencyArtifact.outputFileName
            )
            currentModuleImports.add(dependencyImport)
        }
    }

    val multimoduleOptions = MultimoduleCompileOptions(
        stdlibModuleNameForImport = stdlibModuleName,
        dependencyModules = currentModuleImports,
        initializeUnit = false,
    )

    return WasmIrModuleConfiguration(
        wasmCompiledFileFragments = currentCodeFragments,
        moduleName = artifact.moduleName,
        configuration = configuration,
        typeScriptFragment = null,
        baseFileName = artifact.outputFileName,
        multimoduleOptions = multimoduleOptions,
    )
}

private fun compileStdlibArtifact(
    artifact: WasmModuleArtifactMultimodule,
    loadedFragments: List<WasmIrProgramFragmentsMultimodule>,
    configuration: CompilerConfiguration,
): WasmIrModuleConfiguration {
    val codeFragments = loadedFragments.map { fragment ->
        WasmCompiledCodeFileFragment(fragment.definedTypes, fragment.codeDeclarations, fragment.linkerData)
    }

    val multimoduleOptions = MultimoduleCompileOptions(
        stdlibModuleNameForImport = null,
        dependencyModules = emptySet(),
        initializeUnit = true,
    )
    return WasmIrModuleConfiguration(
        wasmCompiledFileFragments = codeFragments,
        moduleName = artifact.moduleName,
        configuration = configuration,
        typeScriptFragment = null,
        baseFileName = artifact.outputFileName,
        multimoduleOptions = multimoduleOptions,
    )
}

fun compileIncrementallyMultimodule(
    artifacts: List<WasmModuleArtifactMultimodule>,
    configuration: CompilerConfiguration
): List<WasmIrModuleConfiguration> {
    val stdLibArtifact = artifacts.first { it.moduleName == "<kotlin>" }

    val (toRecompile, toDependency) = artifacts.partition { artifact ->
        artifact.fileArtifacts.any { it.isModified() }
    }

    val builtInStdlibFragment = mutableListOf<WasmIrProgramFragmentsMultimodule>()
    val dependencyFragments = mutableMapOf<WasmModuleArtifactMultimodule, List<WasmCompiledDependencyFileFragment>>()
    for (dependency in toDependency) {
        dependencyFragments[dependency] = dependency.loadDependency(
            builtInStdlibFragments = builtInStdlibFragment,
            isStdlibArtifact = dependency == stdLibArtifact
        )
    }

    val recompileFragments = mutableMapOf<WasmModuleArtifactMultimodule, List<WasmIrProgramFragmentsMultimodule>>()
    for (recompile in toRecompile) {
        val (loadedToRecompile, loadedDependency) = recompile.loadRecompileAndDependency(
            builtInStdlibFragment = builtInStdlibFragment,
            isStdlibArtifact = recompile == stdLibArtifact
        )
        recompileFragments[recompile] = loadedToRecompile
        dependencyFragments[recompile] = loadedDependency
    }

    val dependencyResolutionMap = parseDependencyResolutionMap(configuration)

    return recompileFragments.map { (currentArtifact, currentModuleCodeArtifact) ->
        if (currentArtifact == stdLibArtifact) {
            compileStdlibArtifact(
                artifact = currentArtifact,
                loadedFragments = currentModuleCodeArtifact,
                configuration = configuration
            )
        } else {
            compileArtifact(
                artifact = currentArtifact,
                dependencyFragments = dependencyFragments,
                loadedFragments = currentModuleCodeArtifact,
                dependencyResolutionMap = dependencyResolutionMap,
                builtInStdlibFragment = builtInStdlibFragment,
                configuration = configuration,
                stdlibModuleName = stdLibArtifact.moduleName,
            )
        }
    }
}