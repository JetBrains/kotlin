/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import java.io.File
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractCompilerBasedTestForFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.handlers.AbstractFirIdenticalChecker
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.llFirTestDataFile

abstract class AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataTest : AbstractCompilerBasedTestForFir() {
    override fun TestConfigurationBuilder.configureTest() {
        baseFirDiagnosticTestConfiguration(
            frontendFacade = ::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder)
        )

        useAfterAnalysisCheckers(::FirReversedSuppressor)
        useMetaTestConfigurators(::ReversedDiagnosticsConfigurator)
        useAfterAnalysisCheckers(::ReversedFirIdenticalChecker)
    }
}

internal class ReversedDiagnosticsConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun transformTestDataPath(testDataFileName: String): String {
        val reversedTestDataFileName = testDataFileName.replaceFirst(".", ".reversed.")
        return if (File(reversedTestDataFileName).exists()) reversedTestDataFileName else testDataFileName
    }
}

internal class FirReversedSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer> get() = listOf(Companion)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (!isDisabled()) {
            return failedAssertions
        }

        return if (failedAssertions.isEmpty()) {
            listOf(
                AssertionError(
                    "Test contains $IGNORE_REVERSED_RESOLVE directive but no errors was reported. Please remove directive",
                ).wrap()
            )
        } else {
            emptyList()
        }
    }

    private fun isDisabled(): Boolean = IGNORE_REVERSED_RESOLVE in testServices.moduleStructure.allDirectives

    companion object : SimpleDirectivesContainer() {
        val IGNORE_REVERSED_RESOLVE by directive("Temporary disables reversed resolve checks until the issue is fixed")
    }
}

class ReversedFirIdenticalChecker(testServices: TestServices) : AbstractFirIdenticalChecker(testServices) {
    override fun checkTestDataFile(testDataFile: File) {
        if (".reversed." !in testDataFile.path) return

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