/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractCompilerBasedTestForFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.addIdeTestIgnoreHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.runners.baseFirSpecDiagnosticTestConfiguration

abstract class AbstractDiagnosisCompilerTestDataSpecTest : AbstractCompilerBasedTestForFir() {
    override fun TestConfigurationBuilder.configureTest() {
        baseFirDiagnosticTestConfiguration(frontendFacade = ::LowLevelFirFrontendFacade)
        baseFirSpecDiagnosticTestConfiguration()
        addIdeTestIgnoreHandler()
    }
}
