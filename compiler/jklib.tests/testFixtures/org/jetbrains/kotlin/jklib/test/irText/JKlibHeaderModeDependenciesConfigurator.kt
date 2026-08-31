/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class JKlibHeaderModeDependenciesConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {

    private fun isDependencyModule(module: TestModule): Boolean {
        val allModules = testServices.moduleStructure.modules
        return allModules.any { otherModule ->
            otherModule != module && otherModule.allDependencies.any { it.dependencyModule == module }
        }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (isDependencyModule(module)) {
            val existingLvs = configuration.languageVersionSettings
            val updatedFlags = mapOf<AnalysisFlag<*>, Any?>(
                AnalysisFlags.headerMode to true,
                AnalysisFlags.headerModeType to HeaderMode.COMPILATION,
            )
            configuration.languageVersionSettings = LanguageVersionSettingsImpl(
                existingLvs.languageVersion,
                existingLvs.apiVersion,
                updatedFlags,
                existingLvs.getCustomizedLanguageFeatures(),
            )
        }
    }
}
