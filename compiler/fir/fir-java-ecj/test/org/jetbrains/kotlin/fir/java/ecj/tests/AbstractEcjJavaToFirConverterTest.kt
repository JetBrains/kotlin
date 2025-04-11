/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.configuration.setupHandlersForDiagnosticTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

/**
 * Test class for comparing the conversion made by the FirLazyJava* classes with the ECJ-based implementation.
 */
abstract class AbstractEcjJavaToFirConverterTest : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            val baseDir = "."
            globalDefaults {
                frontend = FrontendKinds.FIR
                targetPlatform = JvmPlatforms.defaultJvmPlatform
                dependencyKind = DependencyKind.Source
            }

            defaultDirectives {
                LANGUAGE + "+EnableDfaWarningsInK2"
                +WITH_STDLIB
            }

            enableMetaInfoHandler()

            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::JvmEnvironmentConfigurator,
            )

            configureFirParser(FirParser.Psi)
            
            facadeStep(::EcjJavaToFirConverterFacade)
            firHandlersStep {
                setupHandlersForDiagnosticTest()
            }
            namedHandlersStep("EcjJavaToFirDiagnosticHandlerStep", EcjJavaToFirCompilationArtifact.Kind) {
                useHandlers(::EcjJavaToFirResultsHandler)
            }
        }
    }
}