/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.TestTierDirectives
import org.jetbrains.kotlin.test.directives.TestTierDirectives.DISABLE_NEXT_TIER_SUGGESTION
import org.jetbrains.kotlin.test.directives.TestTierDirectives.LATEST_EXPECTED_TIER
import org.jetbrains.kotlin.test.directives.TestTierDirectives.RUN_PIPELINE_TILL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.latestLVTestDataFile
import org.jetbrains.kotlin.test.utils.llFirTestDataFile
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import org.jetbrains.kotlin.test.utils.reversedTestDataFile
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.File

class PhasedPipelineChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val order: Order
        get() = Order.Last

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestTierDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val (suppressibleFailures, nonSuppressibleFailures, hasFailuresInNonLeafModule) = sortFailures(failedAssertions)

        return nonSuppressibleFailures + when {
            suppressibleFailures.isEmpty() && !hasFailuresInNonLeafModule -> checkTierConsistency()
            else -> emptyList()
        }
    }

    private fun TestArtifactKind<*>.toTier(): TestTierLabel? = when (this) {
        is FrontendKind -> TestTierLabel.FRONTEND
        is BackendKind -> TestTierLabel.FIR2IR
        is ArtifactKind -> TestTierLabel.BACKEND
        else -> null
    }

    private fun checkTierConsistency(): List<WrappedException> {
        val directives = testServices.moduleStructure.allDirectives
        if (DISABLE_NEXT_TIER_SUGGESTION in directives) return emptyList()
        val expectedLastTier = directives[LATEST_EXPECTED_TIER].first()
        val targetedTier = directives[RUN_PIPELINE_TILL].first()
        if (targetedTier >= expectedLastTier) return emptyList()

        val message = "Tier $targetedTier could be promoted to $expectedLastTier"
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        if (testDataFile.extension == "nkt") {
            return listOf(WrappedException.FromAfterAnalysisChecker(AssertionError(message)))
        }

        val originalFile = testDataFile.originalTestDataFile
        return listOf(
            originalFile,
            originalFile.firTestDataFile,
            originalFile.llFirTestDataFile,
            originalFile.latestLVTestDataFile,
            originalFile.reversedTestDataFile,
        ).filter { it.exists() }.mapNotNull { file ->
            val contentWithNewDirective = file
                .readText()
                .replace("// RUN_PIPELINE_TILL: $targetedTier", "// RUN_PIPELINE_TILL: $expectedLastTier")
            try {
                testServices.assertions.assertEqualsToFile(
                    file,
                    contentWithNewDirective,
                    message = { message }
                )
                null
            } catch (e: AssertionError) {
                WrappedException.FromAfterAnalysisChecker(e)
            }
        }
    }

    private data class SortedFailures(
        val suppressibleFailures: List<WrappedException>,
        val nonSuppressibleFailures: List<WrappedException>,
        val hasFailuresInNonLeafModule: Boolean,
    )

    private fun sortFailures(failedAssertions: List<WrappedException>): SortedFailures {
        val suppressibleFailures = mutableListOf<WrappedException>()
        val nonSuppressibleFailures = mutableListOf<WrappedException>()
        val targetedTier = testServices.moduleStructure.allDirectives[RUN_PIPELINE_TILL].first()
        var hasFailuresInNonLeafModule = false

        fun processFailure(module: TestModule?, kind: TestArtifactKind<*>, exception: WrappedException): MutableList<WrappedException> {
            val actualTier = kind.toTier()
            return when {
                module != null && !module.isLeafModule(testServices) -> {
                    hasFailuresInNonLeafModule = true
                    nonSuppressibleFailures
                }
                actualTier == null -> nonSuppressibleFailures
                actualTier == targetedTier -> when {
                    exception is WrappedException.FromHandler && exception.failureDisablesNextSteps -> suppressibleFailures
                    else -> nonSuppressibleFailures
                }
                actualTier > targetedTier -> suppressibleFailures
                actualTier < targetedTier -> nonSuppressibleFailures
                else -> shouldNotBeCalled()
            }
        }


        for (exception in failedAssertions) {
            val targetStorage = when (exception) {
                is WrappedException.FromMetaInfoHandler -> nonSuppressibleFailures
                is WrappedException.FromFacade ->
                    processFailure(exception.failedModule, exception.facade.outputKind, exception)
                is WrappedException.WrappedExceptionWithoutModule -> nonSuppressibleFailures
                is WrappedException.FromHandler ->
                    processFailure(exception.failedModule, exception.handler.artifactKind, exception)
            }
            targetStorage += exception
        }
        return SortedFailures(
            suppressibleFailures = suppressibleFailures,
            nonSuppressibleFailures = nonSuppressibleFailures,
            hasFailuresInNonLeafModule
        )
    }
}
