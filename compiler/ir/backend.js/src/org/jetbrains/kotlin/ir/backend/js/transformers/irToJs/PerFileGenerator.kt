/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.putToMultiMap

typealias CachedTestFunctionsWithTheirPackage = Map<String, List<String>>

interface PerFileGenerator<Module, File, Artifact> {
    val mainModuleName: String

    val Module.isMain: Boolean
    val Module.fileList: Iterable<File>

    val Artifact.artifactName: String
    val Artifact.hasEffect: Boolean
    val Artifact.hasExport: Boolean
    val Artifact.packageFqn: String
    val Artifact.mainFunction: String?

    fun Artifact.takeTestEnvironmentOwnership(): JsIrProgramTestEnvironment?

    fun List<Artifact>.merge(): Artifact
    fun File.generateArtifact(module: Module): Artifact?
    fun Module.generateArtifact(
        mainFunctionTag: String?,
        suiteFunctionTag: String?,
        testFunctions: CachedTestFunctionsWithTheirPackage,
        moduleNameForEffects: String?
    ): Artifact

    fun generatePerFileArtifacts(modules: List<Module>): List<Artifact> {
        var someModuleHasEffect = false

        val nameToModulePerFile = buildMap {
            for (module in modules) {
                var hasModuleLevelEffect = false
                var hasFileWithExportedDeclaration = false
                var suiteFunctionTag: String? = null
                val testFunctions = mutableMapOf<String, MutableList<String>>()

                val artifacts = module.fileList.mapNotNull {
                    val generatedArtifact = it.generateArtifact(module) ?: return@mapNotNull null

                    if (generatedArtifact.hasExport) {
                        hasFileWithExportedDeclaration = true
                    }

                    if (generatedArtifact.hasEffect) {
                        hasModuleLevelEffect = true
                    }

                    generatedArtifact.takeTestEnvironmentOwnership()?.let { (testFunction, suiteFunction) ->
                        testFunctions.putToMultiMap(generatedArtifact.packageFqn, testFunction)
                        suiteFunctionTag = suiteFunction
                    }

                    putToMultiMap(generatedArtifact.artifactName, generatedArtifact)

                    generatedArtifact
                }

                if (hasModuleLevelEffect) {
                    someModuleHasEffect = true
                }

                val mainFunctionTag = runIf(module.isMain) {
                    JsMainFunctionDetector.pickMainFunctionFromCandidates(artifacts) {
                        JsMainFunctionDetector.MainFunctionCandidate(
                            it.packageFqn,
                            it.mainFunction
                        )
                    }?.mainFunction
                }

                if (mainFunctionTag != null || hasFileWithExportedDeclaration || hasModuleLevelEffect || suiteFunctionTag != null || (module.isMain && someModuleHasEffect)) {
                    val proxyArtifact = module.generateArtifact(
                        mainFunctionTag,
                        suiteFunctionTag,
                        testFunctions,
                        mainModuleName.takeIf { !module.isMain && hasModuleLevelEffect }
                    ) ?: continue
                    putToMultiMap(proxyArtifact.artifactName, proxyArtifact)
                }
            }
        }

        return nameToModulePerFile.values.map { it.merge() }
    }
}