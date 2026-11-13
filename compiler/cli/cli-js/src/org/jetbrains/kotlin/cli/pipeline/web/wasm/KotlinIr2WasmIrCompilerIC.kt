/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.MultimoduleCompileOptions
import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.WasmModuleDependencyImport
import org.jetbrains.kotlin.backend.wasm.ic.*
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.WasmStructDeclaration
import org.jetbrains.kotlin.wasm.ir.WasmTypeDeclaration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.wasm.config.wasmGenerateClosedWorldMultimodule
import org.jetbrains.kotlin.wasm.config.wasmHotReload
import org.jetbrains.kotlin.wasm.config.wasmIcGenerateUnchangedModules
import org.jetbrains.kotlin.wasm.config.wasmIncludedModuleOnly

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

private class LoadedRecompileFragments(
    /** files whose declarations this run DEFINES in the emitted module */
    val codeFragments: List<WasmIrProgramFragmentsMultimodule>,
    /** hot-reload delta: files left clean by the IC — not defined here,
     *  imported from the live patch instance instead (see
     *  compileArtifactMultimodule) */
    val cleanFragments: List<WasmIrProgramFragmentsMultimodule>,
    /** the FULL import surface (all files) — other modules may import any
     *  of it; the delta only narrows what is DEFINED, never what is
     *  importable */
    val dependencyFragments: List<WasmCompiledDependencyFileFragment>,
)

/** Worklist-closes [referenced] over the supertype chains of the structs
 *  in [definitions]: a referenced struct's supertype becomes referenced
 *  too, transitively (wasm requires declared supertypes; the chain is
 *  acyclic so the worklist terminates). */
private fun closeReferencedTypesOverSuperTypes(
    referenced: MutableSet<IdSignature>,
    definitions: Map<IdSignature, WasmTypeDeclaration>,
) {
    if (referenced.isEmpty() || definitions.isEmpty()) return
    val pending = ArrayDeque(referenced.toList())
    while (pending.isNotEmpty()) {
        val superTypeSymbol = (definitions[pending.removeLast()] as? WasmStructDeclaration)?.superType as? GcHeapTypeSymbol ?: continue
        if (referenced.add(superTypeSymbol.type)) pending.add(superTypeSymbol.type)
    }
}

private fun WasmModuleArtifactMultimodule.loadRecompileAndDependency(
    builtInFragments: MutableList<WasmIrProgramFragmentsMultimodule>,
    isBuiltInFragments: Boolean,
    isCodeFile: (WasmSrcFileArtifactMultimodule) -> Boolean = { true },
): LoadedRecompileFragments {
    val loadedCodeFragments = mutableListOf<WasmIrProgramFragmentsMultimodule>()
    val loadedCleanFragments = mutableListOf<WasmIrProgramFragmentsMultimodule>()
    val loadedDependencyFragments = mutableListOf<WasmCompiledDependencyFileFragment>()
    for (fileArtifact in this.fileArtifacts) {
        val loadedFragment = fileArtifact.loadIrFragments() ?: continue
        if (isCodeFile(fileArtifact)) {
            loadedCodeFragments.add(loadedFragment)
        } else {
            loadedCleanFragments.add(loadedFragment)
        }
        val dependencyFragment = WasmCompiledDependencyFileFragment(
            definedTypes = loadedFragment.definedTypes,
            definedDeclarations = loadedFragment.dependencyDeclarations
        )
        loadedDependencyFragments.add(dependencyFragment)

        if (isBuiltInFragments && dependencyFragment.hasBuiltinSignature()) {
            builtInFragments.add(loadedFragment)
        }
    }

    return LoadedRecompileFragments(loadedCodeFragments, loadedCleanFragments, loadedDependencyFragments)
}

private fun compileArtifactMultimodule(
    artifact: WasmModuleArtifactMultimodule,
    dependencyFragments: Map<WasmModuleArtifactMultimodule, List<WasmCompiledDependencyFileFragment>>,
    loadedFragments: List<WasmIrProgramFragmentsMultimodule>,
    cleanFragments: List<WasmIrProgramFragmentsMultimodule>,
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

    // Close the referenced types over supertype chains before any
    // projection: wasm requires a kept struct's supertype to be declared,
    // and hot reload's payload/vtable structs chain across FILES (a
    // class's payload subtypes its SUPERCLASS's payload — Int's in the
    // stdlib subtypes Number's). Only this site sees the whole dependency
    // universe, so only here can cross-file chains close; the per-file
    // closure in makeProjection handles intra-file chains for every path.
    val fullGcTypeDeclarations = mutableMapOf<IdSignature, WasmTypeDeclaration>()
    val fullVTableTypeDeclarations = mutableMapOf<IdSignature, WasmStructDeclaration>()
    for (fragments in dependencyFragments.values) {
        for (fragment in fragments) {
            for (entry in fragment.definedTypes.definedGcTypes) {
                fullGcTypeDeclarations.putIfAbsent(entry.key, entry.value)
            }
            for (entry in fragment.definedTypes.definedVTableGcTypes) {
                fullVTableTypeDeclarations.putIfAbsent(entry.key, entry.value)
            }
        }
    }
    closeReferencedTypesOverSuperTypes(currentModuleTypeReferences.gcTypes, fullGcTypeDeclarations)
    closeReferencedTypesOverSuperTypes(currentModuleTypeReferences.gcTypes, fullVTableTypeDeclarations)

    val currentModuleDeclarationReferences = loadedFragments.collectDeclarationReferences { it.referencedDeclarations }
    currentModuleDeclarationReferences.functions.addAll(linkerFunctionSignatures)

    val currentModuleImports = mutableSetOf<WasmModuleDependencyImport>()

    val referencedModules = loadedFragments.flatMapTo(mutableSetOf()) { it.referencedModules }
    referencedModules.add(stdlibModuleName)
    for ([dependencyArtifact, dependencyFragments] in dependencyFragments) {
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

    // HOT-RELOAD DELTA (file granularity; no flag — a cold cache makes
    // every file dirty, so the whole-module loop is just the all-dirty
    // case): the files the IC left clean are not re-emitted — the
    // declarations the dirty files reference become imports under the
    // module's OWN name, which the host maps to the live patch chain.
    // Imports bind to the owner's slot-reading entry, so redefinitions
    // still reach this module's callers through the sweep.
    var hasImportsFromSelf = false
    for (cleanFragment in cleanFragments) {
        val projectedTypes = cleanFragment.definedTypes.makeProjection(currentModuleTypeReferences)
        val projectedDeclarations = cleanFragment.dependencyDeclarations.makeProjection(currentModuleDeclarationReferences)
        val projectedFragment = WasmCompiledDependencyFileFragment(
            definedTypes = projectedTypes,
            definedDeclarations = projectedDeclarations,
        )
        hasImportsFromSelf = hasImportsFromSelf || projectedFragment.definedDeclarations.hasDeclarations
        currentCodeFragments.add(projectedFragment)
    }
    if (hasImportsFromSelf) {
        currentModuleImports.add(
            WasmModuleDependencyImport(
                name = artifact.moduleName,
                fileName = dependencyResolutionMap[artifact.moduleName]
                    ?: artifact.outputFileName
            )
        )
    }

    val multimoduleOptions = MultimoduleCompileOptions(
        stdlibModuleNameForImport = stdlibModuleName,
        dependencyModules = currentModuleImports,
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
    val kotlinTestArtifact = artifacts.firstOrNull { it.moduleName == "<kotlin-test>" }

    val [toRecompile, toDependency] = artifacts.partition { artifact ->
        configuration.wasmIcGenerateUnchangedModules || artifact.forceRebuildWasm || artifact.fileArtifacts.any { it.isModified() }
    }

    val builtInFragments = mutableListOf<WasmIrProgramFragmentsMultimodule>()
    val dependencyFragments = mutableMapOf<WasmModuleArtifactMultimodule, List<WasmCompiledDependencyFileFragment>>()
    val recompileFragments = mutableMapOf<WasmModuleArtifactMultimodule, List<WasmIrProgramFragmentsMultimodule>>()
    val cleanFileFragments = mutableMapOf<WasmModuleArtifactMultimodule, List<WasmIrProgramFragmentsMultimodule>>()
    for (recompile in toRecompile) {
        // Hot reload makes the delta the compilation unit at FILE
        // granularity: an app artifact's files left clean by the IC are
        // imported from the live patch instance instead of re-emitted
        // (see compileArtifactMultimodule). The stdlib stays whole — it
        // is the cold base by design.
        val isDeltaArtifact = configuration.wasmHotReload &&
                recompile != stdLibArtifact && recompile != kotlinTestArtifact
        val loaded = recompile.loadRecompileAndDependency(
            builtInFragments = builtInFragments,
            isBuiltInFragments = (recompile == stdLibArtifact || recompile == kotlinTestArtifact),
            isCodeFile = if (isDeltaArtifact) { fileArtifact -> fileArtifact.isModified() } else { _ -> true },
        )
        recompileFragments[recompile] = loaded.codeFragments
        if (isDeltaArtifact) {
            cleanFileFragments[recompile] = loaded.cleanFragments
        }
        dependencyFragments[recompile] = loaded.dependencyFragments
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

    return recompileFragments.map { [currentArtifact, currentModuleCodeArtifact] ->
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
                cleanFragments = cleanFileFragments[currentArtifact].orEmpty(),
                dependencyResolutionMap = dependencyResolutionMap,
                builtInFragments = builtInFragments,
                configuration = configuration,
                stdlibModuleName = stdLibArtifact.moduleName,
            )
        }
    }
}

fun compileIncrementallySingleModule(
    artifacts: List<WasmModuleArtifactSingleModule>,
    configuration: CompilerConfiguration
): List<WasmIrModuleConfiguration> {
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
    artifacts: List<WasmModuleArtifact>,
    configuration: CompilerConfiguration
): List<WasmIrModuleConfiguration> {
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
