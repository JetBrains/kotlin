/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.konan.config.konanFriendLibraries
import org.jetbrains.kotlin.konan.config.konanHome
import org.jetbrains.kotlin.konan.config.konanLibraries
import org.jetbrains.kotlin.konan.config.konanProducedArtifactKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File

class NativeFirstStageEnvironmentConfigurator(testServices: TestServices, private val customNativeHome: File? = null) :
    NativeEnvironmentConfigurator(testServices, customNativeHome)
{
    override val compilationStage: CompilationStage
        get() = CompilationStage.FIRST

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (!module.targetPlatform(testServices).isNative()) return

        customNativeHome?.let {
            configuration.konanHome = it.absolutePath
            System.setProperty("kotlin.native.home", it.absolutePath) // TODO KT-84799: remove the line after dropping forward testing against 2.3 compiler
        }
        configuration.konanProducedArtifactKind = CompilerOutputKind.LIBRARY
        val klibService = testServices.klibEnvironmentConfigurator
        configuration.put(
            NativeConfigurationKeys.KONAN_OUTPUT_PATH,
            klibService.getKlibArtifactFile(testServices, module.name).absolutePath
        )
        configuration.put(NativeConfigurationKeys.KONAN_DONT_COMPRESS_KLIB, true)
        val include = mutableListOf<String>()
        val friendInclude = mutableListOf<String>()
        for ((dependencyModule, _, relation) in module.allDependencies) {
            if (relation == DependencyRelation.DependsOnDependency) continue
            val dependencyKlib = testServices.artifactsProvider.getArtifact(dependencyModule, ArtifactKinds.KLib).outputFile.absolutePath
            include += dependencyKlib
            if (relation == DependencyRelation.FriendDependency) {
                friendInclude += dependencyKlib
            }
        }
        configuration.konanLibraries += include
        configuration.konanFriendLibraries += friendInclude

        if (testServices.cliBasedFacadesEnabled) {
            configuration.addSourcesForDependsOnClosure(module, testServices)
        }
    }
}
