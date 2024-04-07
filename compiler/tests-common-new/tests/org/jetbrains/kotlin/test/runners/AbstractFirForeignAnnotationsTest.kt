/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.firFrontendStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.handlers.FirJspecifyDiagnosticComplianceHandler
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.preprocessors.JspecifyMarksCleanupPreprocessor
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator

abstract class AbstractFirForeignAnnotationsTestBase(
    kind: ForeignAnnotationsTestKind,
    val parser: FirParser
) : AbstractForeignAnnotationsTestBase(kind) {
    override fun TestConfigurationBuilder.configureFrontend() {
        globalDefaults {
            frontend = FrontendKinds.FIR
        }

        defaultDirectives {
            LanguageSettingsDirectives.LANGUAGE with "+EnableDfaWarningsInK2"
        }

        configureFirParser(parser)

        useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)
        firFrontendStep()
        firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler,
                ::FirDumpHandler,
                ::FirCfgDumpHandler,
                ::FirCfgConsistencyHandler,
                ::FirResolvedTypesVerifier,
            )
        }

        forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/java8Tests/jspecify/*") {
            configureFirHandlersStep {
                useHandlers(::FirJspecifyDiagnosticComplianceHandler)
            }
            useSourcePreprocessor(::JspecifyMarksCleanupPreprocessor)
        }

        forTestsMatching("compiler/testData/diagnostics/*") {
            useAfterAnalysisCheckers(::FirFailingTestSuppressor)
            useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
        }
    }
}

abstract class AbstractFirPsiForeignAnnotationsSourceJavaTest :
    AbstractFirForeignAnnotationsTestBase(ForeignAnnotationsTestKind.SOURCE, FirParser.Psi)

abstract class AbstractFirPsiForeignAnnotationsCompiledJavaTest :
    AbstractFirForeignAnnotationsTestBase(ForeignAnnotationsTestKind.COMPILED_JAVA, FirParser.Psi)

abstract class AbstractFirPsiForeignAnnotationsCompiledJavaWithPsiClassReadingTest :
    AbstractFirForeignAnnotationsTestBase(ForeignAnnotationsTestKind.COMPILED_JAVA_WITH_PSI_CLASS_LOADING, FirParser.Psi)
