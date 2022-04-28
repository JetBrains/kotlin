/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.firFrontendStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.frontend.classic.handlers.FirJspecifyDiagnosticComplianceHandler
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.preprocessors.JspecifyMarksCleanupPreprocessor
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator

abstract class AbstractFirForeignAnnotationsTestBase(kind: ForeignAnnotationsTestKind) : AbstractForeignAnnotationsTestBase(kind) {
    override fun TestConfigurationBuilder.configureFrontend() {
        globalDefaults {
            frontend = FrontendKinds.FIR
        }

        defaultDirectives {
            LanguageSettingsDirectives.LANGUAGE with "+EnableDfaWarningsInK2"
        }

        useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)
        firFrontendStep()
        firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler,
                ::FirDumpHandler,
                ::FirCfgDumpHandler,
                ::FirCfgConsistencyHandler,
                ::FirNoImplicitTypesHandler,
            )
        }

        forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/java8Tests/jspecify/*") {
            configureFirHandlersStep {
                useHandlers(::FirJspecifyDiagnosticComplianceHandler)
            }
            useSourcePreprocessor(::JspecifyMarksCleanupPreprocessor)
        }

        forTestsMatching("compiler/testData/diagnostics/*") {
            useAfterAnalysisCheckers(
                ::FirIdenticalChecker,
                ::FirFailingTestSuppressor,
            )
            useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
        }
    }
}

abstract class AbstractFirForeignAnnotationsSourceJavaTest :
    AbstractFirForeignAnnotationsTestBase(ForeignAnnotationsTestKind.SOURCE)

abstract class AbstractFirForeignAnnotationsCompiledJavaTest :
    AbstractFirForeignAnnotationsTestBase(ForeignAnnotationsTestKind.COMPILED_JAVA)

abstract class AbstractFirForeignAnnotationsCompiledJavaWithPsiClassReadingTest :
    AbstractFirForeignAnnotationsTestBase(ForeignAnnotationsTestKind.COMPILED_JAVA_WITH_PSI_CLASS_LOADING)
