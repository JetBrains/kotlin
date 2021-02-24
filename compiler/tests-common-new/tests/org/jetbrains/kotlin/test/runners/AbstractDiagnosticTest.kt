/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.EXPLICIT_API_MODE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.USE_EXPERIMENTAL
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.FirTestDataConsistencyHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator

abstract class AbstractDiagnosticTest : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +USE_PSI_CLASS_FILES_READING
            +REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
        }

        enableMetaInfoHandler()

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
            ::ScriptingEnvironmentConfigurator
        )

        useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useFrontendFacades(::ClassicFrontendFacade)
        useFrontendHandlers(
            ::DeclarationsDumpHandler,
            ::ClassicDiagnosticsHandler,
        )

        useAfterAnalysisCheckers(::FirTestDataConsistencyHandler)

        forTestsMatching("compiler/testData/diagnostics/testsWithStdLib/*") {
            defaultDirectives {
                +WITH_STDLIB
            }
        }

        forTestsMatching("compiler/testData/diagnostics/tests/testsWithExplicitApi/*") {
            defaultDirectives {
                EXPLICIT_API_MODE with ExplicitApiMode.STRICT
            }
        }

        forTestsMatching("compiler/testData/diagnostics/tests/unsignedTypes/*") {
            defaultDirectives {
                USE_EXPERIMENTAL with "kotlin.ExperimentalUnsignedTypes"
                +WITH_STDLIB
            }
        }

        forTestsMatching("compiler/testData/diagnostics/tests/testsWithJava15/*") {
            defaultDirectives {
                JDK_KIND with TestJdkKind.FULL_JDK_15
                +WITH_STDLIB
                +WITH_REFLECT
            }
        }
    }
}
