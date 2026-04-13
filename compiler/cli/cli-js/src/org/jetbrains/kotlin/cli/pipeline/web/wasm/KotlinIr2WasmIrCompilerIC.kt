/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.MultimoduleCompileOptions
import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.WasmModuleDependencyImport
import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragmentsMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragmentsSingleModule
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactMultimoduleBase
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactSingleModule
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.wasm.config.wasmGenerateClosedWorldMultimodule
import org.jetbrains.kotlin.wasm.config.wasmIncludedModuleOnly
import kotlin.collections.mutableSetOf
import kotlin.collections.set

enum class WasmCompilationMode {
    REGULAR,
    MULTI_MODULE,
    SINGLE_MODULE;

    companion object {
        fun CompilerConfiguration.wasmCompilationMode(): WasmCompilationMode = when {
            wasmIncludedModuleOnly -> SINGLE_MODULE
            wasmGenerateClosedWorldMultimodule -> MULTI_MODULE
            else -> REGULAR
        }
    }
}

private val WasmModuleArtifactMultimoduleBase.outputFileName: String
    get() = externalModuleName ?: encodeModuleName(moduleName)

private val linkerFunctionSignatures = setOf(
    Synthetics.Functions.registerModuleDescriptorBuiltIn.value,
    Synthetics.Functions.createStringBuiltIn.value,
    Synthetics.Functions.tryGetAssociatedObjectBuiltIn.value,
    Synthetics.Functions.jsToKotlinStringAdapterBuiltIn.value,
    Synthetics.Functions.jsToKotlinAnyAdapterBuiltIn.value,
    Synthetics.Functions.runRootSuitesBuiltIn.value,
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
    builtInFragments: MutableList<WasmIrProgramFragmentsMultimodule>,
    isBuiltInFragments: Boolean
): List<WasmCompiledDependencyFileFragment> {
    val loadedDependencies = mutableListOf<WasmCompiledDependencyFileFragment>()
    for (fileArtifact in this.fileArtifacts) {
        val loadedFragment = fileArtifact.loadIrDependencyFragments() ?: continue
        loadedDependencies.add(loadedFragment)

        if (isBuiltInFragments && loadedFragment.hasBuiltinSignature()) {
            builtInFragments.add(fileArtifact.loadIrFragments()!!)
        }
    }
    return loadedDependencies
}

private fun WasmModuleArtifactMultimodule.loadRecompileAndDependency(
    builtInFragments: MutableList<WasmIrProgramFragmentsMultimodule>,
    isBuiltInFragments: Boolean
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

        if (isBuiltInFragments && dependencyFragment.hasBuiltinSignature()) {
            builtInFragments.add(loadedFragment)
        }
    }

    return loadedFragments to loadedDependencyFragments
}

private fun compileArtifactMultimodule(
    artifact: WasmModuleArtifactMultimodule,
    dependencyFragments: Map<WasmModuleArtifactMultimodule, List<WasmCompiledDependencyFileFragment>>,
    loadedFragments: List<WasmIrProgramFragmentsMultimodule>,
    dependencyResolutionMap: Map<String, String>,
    builtInFragments: MutableList<WasmIrProgramFragmentsMultimodule>,
    configuration: CompilerConfiguration,
    stdlibModuleName: String,
): WasmIrModuleConfiguration {

    val currentCodeFragments = mutableListOf<WasmCompiledFileFragment>()
    loadedFragments.mapTo(currentCodeFragments) { fragment ->
        WasmCompiledCodeFileFragment(fragment.definedTypes, fragment.codeDeclarations, fragment.linkerData)
    }

    val currentModuleTypeReferences = (loadedFragments + builtInFragments).collectTypeReferences { it.referencedTypes }
    currentModuleTypeReferences.gcTypes.addAll(linkerTypeSignatures)
    currentModuleTypeReferences.functionTypes.addAll(linkerFunctionSignatures)

    val currentModuleDeclarationReferences = loadedFragments.collectDeclarationReferences { it.referencedDeclarations }
    currentModuleDeclarationReferences.functions.addAll(linkerFunctionSignatures)

    val currentModuleImports = mutableSetOf<WasmModuleDependencyImport>()

    val referencedModules = loadedFragments.flatMapTo(mutableSetOf()) { it.referencedModules }
    referencedModules.add(stdlibModuleName)
    for ((dependencyArtifact, dependencyFragments) in dependencyFragments) {
        if (dependencyArtifact == artifact) continue
        if (dependencyArtifact.moduleName !in referencedModules) continue

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

private fun compileStdlibArtifactMultimodule(
    artifact: WasmModuleArtifactMultimoduleBase,
    codeFragments: List<WasmCompiledFileFragment>,
    configuration: CompilerConfiguration,
): WasmIrModuleConfiguration {
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
    moduleArtifacts: List<ModuleArtifact>,
    configuration: CompilerConfiguration
): List<WasmIrModuleConfiguration> {
    val artifacts = moduleArtifacts.filterIsInstance<WasmModuleArtifactMultimodule>()
    check(moduleArtifacts.size == artifacts.size)

    val stdLibArtifact = artifacts.first { it.moduleName == "<kotlin>" }
    val kotlinTestArtifact = artifacts.firstOrNull { it.moduleName == "<kotlin-test>" }

    val (toRecompile, toDependency) = artifacts.partition { artifact ->
        artifact.fileArtifacts.any { it.isModified() }
    }

    val builtInFragments = mutableListOf<WasmIrProgramFragmentsMultimodule>()
    val dependencyFragments = mutableMapOf<WasmModuleArtifactMultimodule, List<WasmCompiledDependencyFileFragment>>()
    val recompileFragments = mutableMapOf<WasmModuleArtifactMultimodule, List<WasmIrProgramFragmentsMultimodule>>()
    for (recompile in toRecompile) {
        val (loadedToRecompile, loadedDependency) = recompile.loadRecompileAndDependency(
            builtInFragments = builtInFragments,
            isBuiltInFragments = (recompile == stdLibArtifact || recompile == kotlinTestArtifact),
        )
        recompileFragments[recompile] = loadedToRecompile
        dependencyFragments[recompile] = loadedDependency
    }

    val allReferencedModules = mutableSetOf<String>()
    allReferencedModules.add(stdLibArtifact.moduleName)
    recompileFragments.values.forEach { fragments ->
        fragments.flatMapTo(allReferencedModules) { it.referencedModules }
    }
    for (dependency in toDependency) {
        if (dependency.moduleName !in allReferencedModules) continue
        dependencyFragments[dependency] = dependency.loadDependency(
            builtInFragments = builtInFragments,
            isBuiltInFragments = (dependency == stdLibArtifact || dependency == kotlinTestArtifact),
        )
    }

    val dependencyResolutionMap = parseDependencyResolutionMap(configuration)

    return recompileFragments.map { (currentArtifact, currentModuleCodeArtifact) ->
        if (currentArtifact == stdLibArtifact) {
            val codeFragments = currentModuleCodeArtifact.map { fragment ->
                WasmCompiledCodeFileFragment(fragment.definedTypes, fragment.codeDeclarations, fragment.linkerData)
            }
            compileStdlibArtifactMultimodule(
                artifact = currentArtifact,
                codeFragments = codeFragments,
                configuration = configuration
            )
        } else {
            compileArtifactMultimodule(
                artifact = currentArtifact,
                dependencyFragments = dependencyFragments,
                loadedFragments = currentModuleCodeArtifact,
                dependencyResolutionMap = dependencyResolutionMap,
                builtInFragments = builtInFragments,
                configuration = configuration,
                stdlibModuleName = stdLibArtifact.moduleName,
            )
        }
    }
}

fun compileIncrementallySingleModule(
    moduleArtifacts: List<ModuleArtifact>,
    configuration: CompilerConfiguration
): List<WasmIrModuleConfiguration> {
    val artifacts = moduleArtifacts.filterIsInstance<WasmModuleArtifactSingleModule>()
    check(moduleArtifacts.size == artifacts.size)

    val stdLibArtifact = artifacts.first { it.moduleName == "<kotlin>" }
    val mainArtifact = artifacts.last()

    val mainModuleCodeFragments = mainArtifact.fileArtifacts.mapNotNull {
        it.loadIrFragments()?.fragmentData as? WasmIrProgramFragmentsSingleModule.Compiled
    }

    val currentCodeFragments = mutableListOf<WasmCompiledFileFragment>()
    mainModuleCodeFragments.mapTo(currentCodeFragments) { it.codeFileFragment }

    if (stdLibArtifact == mainArtifact) {
        val configuration = compileStdlibArtifactMultimodule(
            artifact = mainArtifact,
            codeFragments = currentCodeFragments,
            configuration = configuration
        )
        return listOf(configuration)
    }

    val declarationReferences = mainModuleCodeFragments.collectDeclarationReferences { it.referencedDeclarations }
    declarationReferences.functions.addAll(linkerFunctionSignatures)

    val dependencyResolutionMap = parseDependencyResolutionMap(configuration)
    val currentModuleImports = mutableSetOf<WasmModuleDependencyImport>()
    val dependencyArtifacts = artifacts.filter { it != mainArtifact }

    dependencyArtifacts.forEach { dependencyArtifact ->
        val dependencyFragment = dependencyArtifact.fileArtifacts.mapNotNull {
            (it.loadIrFragments()?.fragmentData as? WasmIrProgramFragmentsSingleModule.Dependency)?.dependencyFragment
        }

        var hasImportsFromDependency = false
        dependencyFragment.forEach {
            val declarationProjection = it.definedDeclarations.makeProjection(declarationReferences)
            val projectedFragment = WasmCompiledDependencyFileFragment(
                definedTypes = it.definedTypes,
                definedDeclarations = declarationProjection,
            )
            currentCodeFragments.add(projectedFragment)
            hasImportsFromDependency = hasImportsFromDependency || projectedFragment.definedDeclarations.hasDeclarations
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
        stdlibModuleNameForImport = stdLibArtifact.moduleName,
        dependencyModules = currentModuleImports,
        initializeUnit = false,
    )

    val configuration = WasmIrModuleConfiguration(
        wasmCompiledFileFragments = currentCodeFragments,
        moduleName = mainArtifact.moduleName,
        configuration = configuration,
        typeScriptFragment = null,
        baseFileName = mainArtifact.outputFileName,
        multimoduleOptions = multimoduleOptions,
    )
    return listOf(configuration)
}

fun compileIncrementallyWholeWorld(
    moduleArtifacts: List<ModuleArtifact>,
    configuration: CompilerConfiguration
): List<WasmIrModuleConfiguration> {
    val artifacts = moduleArtifacts.filterIsInstance<WasmModuleArtifact>()
    check(moduleArtifacts.size == artifacts.size)

    val wasmArtifacts = artifacts
        .flatMap { it.fileArtifacts }
        .mapNotNull { it.loadIrFragments()?.mainFragment }

    val configuration = WasmIrModuleConfiguration(
        wasmCompiledFileFragments = wasmArtifacts,
        moduleName = configuration.moduleName!!,
        configuration = configuration,
        typeScriptFragment = null,
        baseFileName = configuration.outputName!!,
        multimoduleOptions = null,
    )

    return listOf(configuration)
}