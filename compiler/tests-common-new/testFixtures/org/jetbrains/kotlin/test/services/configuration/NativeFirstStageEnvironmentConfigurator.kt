/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.cliBasedFacadesEnabled
import java.io.File

class NativeFirstStageEnvironmentConfigurator(
    testServices: TestServices,
    customNativeHome: File? = null
) : NativeEnvironmentConfigurator(testServices, customNativeHome) {
    override val compilationStage: CompilationStage
        get() = CompilationStage.FIRST

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        super.configureCompilerConfiguration(configuration, module)

        // Load only direct dependencies (i.e. no transitive dependencies).
        val dependencies = module.regularDependencies.map { getKlibArtifactDir(testServices, it.dependencyModule.name).absolutePath }
        val friends = module.friendDependencies.map { getKlibArtifactDir(testServices, it.dependencyModule.name).absolutePath }

        val runtimeDependencies = getRuntimeLibraryProviders(module).flatMap { provider ->
            // Ignore `KlibNativeDistributionLibraryProvider`, because it is anyway applied in loadNativeKlibs().
            if (provider is KlibNativeDistributionLibraryProvider) emptyList() else provider.getLibraryPaths()
        }

        configuration.konanLibraries = runtimeDependencies + dependencies + friends
        configuration.konanFriendLibraries = friends

        configuration.konanProducedArtifactKind = CompilerOutputKind.LIBRARY
        configuration.konanOutputPath = getKlibArtifactDir(testServices, module.name).absolutePath
        configuration.konanDontCompressKlib = true

        if (testServices.cliBasedFacadesEnabled) {
            configuration.addSourcesForDependsOnClosure(module, testServices)
        }
    }
}
