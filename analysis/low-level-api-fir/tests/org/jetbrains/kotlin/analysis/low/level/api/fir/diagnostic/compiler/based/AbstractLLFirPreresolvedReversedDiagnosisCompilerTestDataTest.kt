/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractCompilerBasedTestForFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

abstract class AbstractLLFirPreresolvedReversedDiagnosisCompilerTestDataTest : AbstractCompilerBasedTestForFir() {
    override fun TestConfigurationBuilder.configureTest() {
        baseFirDiagnosticTestConfiguration(
            frontendFacade = ::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder)
        )

        useMetaTestConfigurators(::ReversedDiagnosticsConfigurator)
    }
}

private class ReversedDiagnosticsConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun transformTestDataPath(testDataFileName: String): String {
        val index = testDataFileName.indexOf('.')
        val reversedTestDataFileName = testDataFileName.substring(0, index) +
                ".reversed" +
                testDataFileName.substring(index, testDataFileName.length)

        return if (File(reversedTestDataFileName).exists()) reversedTestDataFileName else testDataFileName
    }
}
