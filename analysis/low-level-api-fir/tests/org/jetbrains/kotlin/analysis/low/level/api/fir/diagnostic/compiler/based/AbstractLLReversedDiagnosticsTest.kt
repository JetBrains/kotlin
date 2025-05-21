/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.CustomOutputDiagnosticsConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirOnlyReversedTestSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractLLCompilerBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.frontend.fir.handlers.AbstractFirIdenticalChecker
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.llFirTestDataFile
import org.jetbrains.kotlin.utils.bind
import java.io.File

/**
 * Checks diagnostics in the test data with reversed resolution order.
 * Example:
 * ```kotlin
 * fun one() = 0
 * fun two() = true
 * ```
 * The function `two` will be resolved first.
 * The function `one` will be resolved after that.
 *
 * A counterpart for [AbstractLLDiagnosticsTest].
 *
 * @see AbstractLLDiagnosticsTest
 */
abstract class AbstractLLReversedDiagnosticsTest : AbstractLLCompilerBasedTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            baseFirDiagnosticTestConfiguration(
                frontendFacade = ::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder),
                testDataConsistencyHandler = ::ReversedFirIdenticalChecker,
            )

            useAfterAnalysisCheckers(::LLFirOnlyReversedTestSuppressor)
            useMetaTestConfigurators(::reversedDiagnosticsConfigurator)
        }
    }
}

internal fun reversedDiagnosticsConfigurator(testServices: TestServices): MetaTestConfigurator {
    return CustomOutputDiagnosticsConfigurator(".reversed.", testServices)
}

class ReversedFirIdenticalChecker(testServices: TestServices) : AbstractFirIdenticalChecker(testServices) {
    override fun checkTestDataFile(testDataFile: File) {
        if (".reversed." !in testDataFile.path) return
        val helper = Helper()
        val originalFile = helper.getClassicFileToCompare(testDataFile).path.replace(".reversed", "").let(::File)
        val baseFile = originalFile.llFirTestDataFile.takeIf(File::exists)
            ?: originalFile.firTestDataFile.takeIf(File::exists)
            ?: originalFile

        val baseContent = helper.readContent(baseFile, trimLines = false)
        val reversedFirContent = helper.readContent(testDataFile, trimLines = false)
        if (baseContent == reversedFirContent) {
            testServices.assertions.fail {
                "`${testDataFile.name}` and `${baseFile.name}` are identical. Remove `$testDataFile`."
            }
        } else {
            assertPreprocessedTestDataAreEqual(testServices, baseFile, baseContent, testDataFile, reversedFirContent) {
                "When ignoring diagnostics, the contents of `${baseFile.name}` (expected) and `${testDataFile.name}` (actual) are not" +
                        " identical. `.reversed.kt` test data may only differ from its base `.fir.kt` or `.kt` test data in the reported" +
                        " diagnostics. Update one of these test data files."
            }
        }
    }
}
