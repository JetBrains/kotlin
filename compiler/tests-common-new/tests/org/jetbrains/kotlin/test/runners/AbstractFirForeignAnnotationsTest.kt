/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firFrontendStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.preprocessors.JspecifyMarksCleanupPreprocessor
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator

private val configureFir: TestConfigurationBuilder.() -> Unit = {
    globalDefaults {
        frontend = FrontendKinds.FIR
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
        // TODO: port JspecifyDiagnosticComplianceHandler (it doesn't really use the frontend artifact)
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

abstract class AbstractFirForeignAnnotationsTestBase : AbstractForeignAnnotationsTestBase(configureFir)

abstract class AbstractFirForeignAnnotationsSourceJavaTest : AbstractFirForeignAnnotationsTestBase()

abstract class AbstractFirForeignAnnotationsCompiledJavaTest :
    AbstractForeignAnnotationsCompiledJavaTest(configureFir)

abstract class AbstractFirForeignAnnotationsCompiledJavaWithPsiClassReadingTest :
    AbstractForeignAnnotationsCompiledJavaWithPsiClassReadingTest(configureFir)
