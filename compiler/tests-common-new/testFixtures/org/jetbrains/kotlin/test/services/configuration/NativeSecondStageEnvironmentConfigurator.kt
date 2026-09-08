/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.config.konanFriendLibraries
import org.jetbrains.kotlin.konan.config.konanIncludedLibraries
import org.jetbrains.kotlin.konan.config.konanLibraries
import org.jetbrains.kotlin.konan.config.konanProducedArtifactKind
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyRelation
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

        // Load all dependencies (including transitive dependencies).
        val dependencies = getKlibDependencies(module, testServices, DependencyRelation.RegularDependency).map { it.absolutePath }
        val friends = getKlibDependencies(module, testServices, DependencyRelation.FriendDependency).map { it.absolutePath }

        val runtimeDependencies = getRuntimeLibraryProviders(module).flatMap { provider ->
            // Ignore `KlibNativeDistributionLibraryProvider`, because it is anyway applied in loadNativeKlibs().
            if (provider is KlibNativeDistributionLibraryProvider) emptyList() else provider.getLibraryPaths()
        }

        val includedLibrary = testServices.artifactsProvider.getArtifact(module, ArtifactKinds.KLib).outputFile.absolutePath

        configuration.konanLibraries = runtimeDependencies + dependencies + friends + includedLibrary
        configuration.konanFriendLibraries = friends
        configuration.konanIncludedLibraries = listOf(includedLibrary)

        configuration.konanProducedArtifactKind = CompilerOutputKind.PROGRAM
    }
}
