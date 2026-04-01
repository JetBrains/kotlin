/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.moduleStructure

class UnsupportedFeaturesTestConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        val testModule = testServices.moduleStructure.modules.first()
        val languageVersion = testServices.compilerConfigurationProvider.getCompilerConfiguration(
            testModule,
            CompilationStage.FIRST
        ).languageVersionSettings.languageVersion

        return testModule.directives[LanguageSettingsDirectives.LANGUAGE]
            .asSequence()
            .mapNotNull { languageFeatureString ->
                val featureName = if (languageFeatureString.startsWith("+")) languageFeatureString.drop(1) else return@mapNotNull null
                LanguageFeature.fromString(featureName) ?: return@mapNotNull null
            }.any { languageFeature ->
                languageFeature != LanguageFeature.ExportKlibToOlderAbiVersion &&
                        languageFeature != LanguageFeature.MultiPlatformProjects &&
                        // JsAllowValueClassesInExternals(sinceVersion = KOTLIN_2_0) is always added in `TestConfigurationBuilder.commonConfigurationForJsBackendSecondStageTest()`,
                        // It actually was fine in 1.9 as well, so let's run the whole testsuit for 1.9 as well.
                        languageFeature != LanguageFeature.JsAllowValueClassesInExternals &&
                        languageFeature != LanguageFeature.JsAllowImplementingFunctionInterface &&
                        !languageFeature.isSupportedInLV(languageVersion)
            }
    }

    private fun LanguageFeature.isSupportedInLV(languageVersion: LanguageVersion): Boolean =
        sinceVersion?.let {
            it.major < languageVersion.major ||
                    (it.major == languageVersion.major && it.minor <= languageVersion.minor)
        } ?: false

    private fun LanguageFeature.isNeededInModule(testModule: TestModule): Boolean =
        testModule.languageVersionSettings.supportsFeature(this)
}

