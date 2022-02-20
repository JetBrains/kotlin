/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.classicFrontendStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.CHECK_COMPILE_TIME_VALUES
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.EXPLICIT_API_MODE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OPT_IN
import org.jetbrains.kotlin.test.frontend.classic.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractDiagnosticTest : AbstractKotlinCompilerTest() {
    companion object {
        val DISABLED_BY_DEFAULT_UNUSED_DIAGNOSTICS = listOf(
            "UNUSED_VARIABLE",
            "UNUSED_PARAMETER",
            "UNUSED_ANONYMOUS_PARAMETER",
            "UNUSED_DESTRUCTURED_PARAMETER_ENTRY",
            "UNUSED_TYPEALIAS_PARAMETER",
            "UNUSED_VALUE",
            "UNUSED_CHANGED_VALUE",
            "UNUSED_EXPRESSION",
            "UNUSED_LAMBDA_EXPRESSION",
        ).map { "-$it" }
    }

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

        classicFrontendStep()

        classicFrontendHandlersStep {
            useHandlers(
                ::DeclarationsDumpHandler,
                ::ClassicDiagnosticsHandler,
                ::ConstantValuesHandler,
                ::DiagnosticMessagesTextHandler
            )
        }

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

        forTestsNotMatching("compiler/testData/diagnostics/tests/controlFlowAnalysis/*") {
            defaultDirectives {
                DIAGNOSTICS with DISABLED_BY_DEFAULT_UNUSED_DIAGNOSTICS
            }
        }

        forTestsMatching("compiler/testData/diagnostics/tests/unsignedTypes/*") {
            defaultDirectives {
                OPT_IN with "kotlin.ExperimentalUnsignedTypes"
                +WITH_STDLIB
            }
        }

        forTestsMatching("compiler/testData/diagnostics/tests/testsWithJava17/*") {
            defaultDirectives {
                JDK_KIND with TestJdkKind.FULL_JDK_17
                +WITH_STDLIB
                +WITH_REFLECT
            }
        }

        forTestsMatching("compiler/testData/diagnostics/tests/multiplatform/*") {
            defaultDirectives {
                LANGUAGE with "+MultiPlatformProjects"
            }
        }

        // ----------------------- constant evaluation tests -----------------------
        forTestsMatching("compiler/testData/diagnostics/tests/constantEvaluator/*") {
            defaultDirectives {
                LANGUAGE with "-ApproximateIntegerLiteralTypesInReceiverPosition"
            }
        }

        forTestsMatching("compiler/testData/diagnostics/tests/constantEvaluator/constant/*") {
            defaultDirectives {
                CHECK_COMPILE_TIME_VALUES with ConstantValuesHandler.Mode.Constant
            }
        }

        forTestsMatching("compiler/testData/diagnostics/tests/constantEvaluator/isPure/*") {
            defaultDirectives {
                CHECK_COMPILE_TIME_VALUES with ConstantValuesHandler.Mode.IsPure
            }
        }

        forTestsMatching("compiler/testData/diagnostics/tests/constantEvaluator/usesVariableAsConstant/*") {
            defaultDirectives {
                CHECK_COMPILE_TIME_VALUES with ConstantValuesHandler.Mode.UsesVariableAsConstant
            }
        }
    }
}
