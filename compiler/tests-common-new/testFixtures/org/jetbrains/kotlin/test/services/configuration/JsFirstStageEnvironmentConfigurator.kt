/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.klibRelativePathBases
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.KLIB_RELATIVE_PATH_BASES
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

open class JsFirstStageEnvironmentConfigurator(testServices: TestServices) : JsEnvironmentConfigurator(testServices) {
    override val compilationStage: CompilationStage
        get() = CompilationStage.FIRST

    companion object : KlibBasedEnvironmentConfiguratorUtils

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (!module.targetPlatform(testServices).isJs()) return

        super.configureCompilerConfiguration(configuration, module)

        configuration.outputDir = getKlibArtifactFile(testServices, module.name)

        val dependencies = module.regularDependencies.map { getKlibArtifactFile(testServices, it.dependencyModule.name).absolutePath }
        val friends = module.friendDependencies.map { getKlibArtifactFile(testServices, it.dependencyModule.name).absolutePath }

        val libraries = when (val targetBackend = testServices.defaultsProvider.targetBackend) {
            null -> listOf(
                testServices.standardLibrariesPathProvider.fullJsStdlib().absolutePath,
                testServices.standardLibrariesPathProvider.kotlinTestJsKLib().absolutePath
            )
            TargetBackend.JS_IR, TargetBackend.JS_IR_ES6 -> getRuntimePathsForModule(module, testServices) + dependencies + friends
            else -> error("Unsupported target backend: $targetBackend")
        }
        configuration.libraries = libraries
        configuration.friendLibraries = friends

        configuration.klibRelativePathBases = module.directives[KLIB_RELATIVE_PATH_BASES].applyIf(testServices.cliBasedFacadesEnabled) {
            val modulePath = testServices.sourceFileProvider.getKotlinSourceDirectoryForModule(module).canonicalPath
            map { "$modulePath/$it" }
        }

        if (testServices.cliBasedFacadesEnabled) {
            configuration.addSourcesForDependsOnClosure(module, testServices)
        }
    }
}
