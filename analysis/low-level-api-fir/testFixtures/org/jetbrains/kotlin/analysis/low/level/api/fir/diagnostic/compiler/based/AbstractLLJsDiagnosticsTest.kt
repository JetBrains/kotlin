/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator

/**
 * Checks Kotlin/JS diagnostics in the test data with regular resolution order.
 *
 * A counterpart for [AbstractLLReversedJsDiagnosticsTest].
 *
 * @see AbstractLLReversedJsDiagnosticsTest
 */
abstract class AbstractLLJsDiagnosticsTest : AbstractLLDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        configureForLLJsDiagnosticsTest()
    }
}


fun TestConfigurationBuilder.configureForLLJsDiagnosticsTest() {
    defaultDirectives {
        +ConfigurationDirectives.WITH_STDLIB
        +FirDiagnosticsDirectives.FIR_IDENTICAL
    }

    globalDefaults {
        targetPlatform = JsPlatforms.defaultJsPlatform
    }

    useConfigurators(
        ::JsFirstStageEnvironmentConfigurator,
    )

    useAdditionalSourceProviders(
        ::JsAdditionalSourceProvider,
    )
}
