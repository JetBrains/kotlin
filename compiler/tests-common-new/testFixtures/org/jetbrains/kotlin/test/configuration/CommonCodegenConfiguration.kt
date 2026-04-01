/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.RENDER_FIR_DECLARATION_ATTRIBUTES

fun TestConfigurationBuilder.commonCodegenConfiguration() {
    configureEvaluateTests()
}

/**
 * Enables FIR dump for tests inside `compiler/testData/codegen/box/evaluate`
 */
 fun TestConfigurationBuilder.configureEvaluateTests() {
    forTestsMatching("compiler/testData/codegen/box(?:Jvm)?/evaluate/*") {
        defaultDirectives {
            +FIR_DUMP
            +RENDER_FIR_DECLARATION_ATTRIBUTES
        }
    }
}
