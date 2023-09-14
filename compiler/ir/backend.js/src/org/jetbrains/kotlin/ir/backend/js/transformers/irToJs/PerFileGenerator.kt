/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.utils.putToMultiMap

interface PerFileGenerator<Module, File, Artifact> {
    val mainModuleName: String

    val Module.isMain: Boolean
    val Module.fileList: Iterable<File>

    val Artifact.artifactName: String
    val Artifact.hasEffect: Boolean
    val Artifact.hasExport: Boolean

    fun List<Artifact>.merge(): Artifact
    fun File.generateArtifact(module: Module): Artifact?
    fun Module.generateArtifact(moduleNameForEffects: String?): Artifact

    fun generatePerFileArtifacts(modules: List<Module>): List<Artifact> {
        var someModuleHasEffect = false

        val nameToModulePerFile = buildMap {
            for (module in modules) {
                var hasModuleLevelEffect = false
                var hasFileWithExportedDeclaration = false

                for (file in module.fileList) {
                    val generatedArtifact = file.generateArtifact(module) ?: continue

                    if (generatedArtifact.hasExport) {
                        hasFileWithExportedDeclaration = true
                    }

                    if (generatedArtifact.hasEffect) {
                        hasModuleLevelEffect = true
                    }

                    putToMultiMap(generatedArtifact.artifactName, generatedArtifact)
                }

                if (hasModuleLevelEffect) {
                    someModuleHasEffect = true
                }

                if (hasFileWithExportedDeclaration || hasModuleLevelEffect || (module.isMain && someModuleHasEffect)) {
                    val proxyArtifact =
                        module.generateArtifact(mainModuleName.takeIf { !module.isMain && hasModuleLevelEffect }) ?: continue
                    putToMultiMap(proxyArtifact.artifactName, proxyArtifact)
                }
            }
        }

        return nameToModulePerFile.values.map { it.merge() }
    }
}
