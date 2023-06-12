/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.SPEC_HELPERS
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.frontend.classic.handlers.FirTestDataConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirIdenticalChecker
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.SpecHelpersSourceFilesProvider

abstract class AbstractFirDiagnosticTestSpecBase(parser: FirParser) : AbstractFirDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            baseFirSpecDiagnosticTestConfiguration()
        }
    }
}

abstract class AbstractFirPsiDiagnosticTestSpec : AbstractFirDiagnosticTestSpecBase(FirParser.Psi)

fun TestConfigurationBuilder.baseFirSpecDiagnosticTestConfiguration(baseDir: String = ".") {
    defaultDirectives {
        +SPEC_HELPERS
        +WITH_STDLIB
    }

    useAdditionalSourceProviders(::SpecHelpersSourceFilesProvider.bind(baseDir))

    useAfterAnalysisCheckers(
        ::FirIdenticalChecker,
        ::FirTestDataConsistencyHandler,
        ::FirFailingTestSuppressor,
    )

    useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
}
