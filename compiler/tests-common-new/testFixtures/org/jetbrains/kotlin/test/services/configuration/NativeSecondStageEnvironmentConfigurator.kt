/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.config.konanIncludedLibraries
import org.jetbrains.kotlin.konan.config.konanLibraries
import org.jetbrains.kotlin.konan.config.konanProducedArtifactKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import kotlin.collections.plus

class NativeSecondStageEnvironmentConfigurator(testServices: TestServices) : NativeEnvironmentConfigurator(testServices, customNativeHome = null) {
    override val compilationStage: CompilationStage
        get() = CompilationStage.SECOND

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        super.configureCompilerConfiguration(configuration, module)

        configuration.konanProducedArtifactKind = CompilerOutputKind.PROGRAM

        val includedLibrary = testServices.artifactsProvider.getArtifact(module, ArtifactKinds.KLib).outputFile.absolutePath

        configuration.konanLibraries += includedLibrary
        configuration.konanIncludedLibraries = listOf(includedLibrary)
    }
}
