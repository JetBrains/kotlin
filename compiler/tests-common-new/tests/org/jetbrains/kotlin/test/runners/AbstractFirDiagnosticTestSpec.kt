/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.SPEC_HELPERS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirIdenticalChecker
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.SpecHelpersSourceFilesProvider

abstract class AbstractFirDiagnosticTestSpec : AbstractFirDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +SPEC_HELPERS
                +WITH_STDLIB
            }

            useAdditionalSourceProviders(::SpecHelpersSourceFilesProvider)

            useAfterAnalysisCheckers(
                ::FirIdenticalChecker,
                ::FirFailingTestSuppressor,
            )

            useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
        }
    }
}
