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

class PhasedPipelineChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val order: Order
        get() = Order.Last

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestTierDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val sortedFailures = sortFailures(failedAssertions)
        if (sortedFailures.criticalFailures.isEmpty()) {
            return sortedFailures.nonCriticalFailures + listOfNotNull(checkTierConsistency())
        }
        return filterCriticalFailures(sortedFailures) ?: failedAssertions
    }

    private fun filterCriticalFailures(sortedFailures: SortedFailures): List<WrappedException>? {
        if (!sortedFailures.suppressibleByPhase) return null
        val (failedModule, stepKind, exceptionToFilter) = sortedFailures.criticalFailureInfo!!
        // TODO: add proper error message
        if (!failedModule.isLeafModule(testServices)) return null
        // TODO: properly handle case without directive
        val targetedTier = testServices.moduleStructure.allDirectives[RUN_PIPELINE_TILL].first()
        val actualTier = when (stepKind) {
            is FrontendKind -> TestTierLabel.FRONTEND
            is BackendKind -> TestTierLabel.FIR2IR
            is ArtifactKind -> TestTierLabel.BACKEND
            else -> return null
        }
        // TODO: add proper error message
        if (actualTier >= targetedTier) return sortedFailures.nonCriticalFailures
        return null
    }

    private fun checkTierConsistency(): WrappedException? {
        val directives = testServices.moduleStructure.allDirectives
        if (DISABLE_NEXT_TIER_SUGGESTION in directives) return null
        val expectedLastTier = directives[LATEST_EXPECTED_TIER].first()
        val targetedTier = directives[RUN_PIPELINE_TILL].first()
        if (targetedTier < expectedLastTier) {
            val exception = RuntimeException("Tier $targetedTier could be promoted to $expectedLastTier")
            return WrappedException.FromAfterAnalysisChecker(exception)
        }
        return null
    }

    private class SortedFailures(
        val criticalFailureInfo: CriticalFailureInfo?,
        val criticalFailures: List<WrappedException>,
        val nonCriticalFailures: List<WrappedException>
    ) {
        val suppressibleByPhase: Boolean
            get() = criticalFailures.size == 1 && criticalFailureInfo != null
    }

    private data class CriticalFailureInfo(
        val failedModule: TestModule,
        val stepKind: TestArtifactKind<*>,
        val exceptionToFilter: WrappedException
    )

    private fun sortFailures(failedAssertions: List<WrappedException>): SortedFailures {
        val criticalFailures = mutableListOf<WrappedException>()
        val nonCriticalFailures = mutableListOf<WrappedException>()
        var criticalFailureInfo: CriticalFailureInfo? = null
        for (exception in failedAssertions) {
            when (exception) {
                is WrappedException.FromMetaInfoHandler,
                is WrappedException.FromFacade -> nonCriticalFailures += exception
                is WrappedException.WrappedExceptionWithoutModule -> criticalFailures += exception
                is WrappedException.FromHandler -> {
                    if (exception.failureDisablesNextSteps) {
                        criticalFailures += exception
                        val failedModule = exception.failedModule
                        if (criticalFailureInfo == null && failedModule != null) {
                            criticalFailureInfo = CriticalFailureInfo(
                                failedModule,
                                exception.handler.artifactKind,
                                exception
                            )
                        }
                    } else {
                        nonCriticalFailures += exception
                    }
                }
            }
        }
        return SortedFailures(
            criticalFailureInfo,
            criticalFailures,
            nonCriticalFailures
        )
    }
}
