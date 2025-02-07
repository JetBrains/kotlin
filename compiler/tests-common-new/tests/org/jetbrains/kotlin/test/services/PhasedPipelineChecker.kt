/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.DISABLE_NEXT_PHASE_SUGGESTION
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.RUN_PIPELINE_TILL
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

class PhasedPipelineChecker(
    testServices: TestServices,
    val defaultRunPipelineTill: TestPhase? = null,
) : AfterAnalysisChecker(testServices) {
    override val order: Order
        get() = Order.P4

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestPhaseDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        checkLatestPhaseDirective()?.let { return failedAssertions + it }
        val targetedPhase = getTargetedPhase()
        if (targetedPhase == null) {
            return failedAssertions + reportMissingDirective(failedAssertions)
        }

        val (suppressibleFailures, nonSuppressibleFailures, hasFailuresInNonLeafModule, hasNonSuppressibleFailuresFromFacade) = sortFailures(failedAssertions)

        return nonSuppressibleFailures + when {
            suppressibleFailures.isEmpty() && !hasNonSuppressibleFailuresFromFacade && !hasFailuresInNonLeafModule -> checkPhaseConsistency()
            else -> emptyList()
        }
    }

    private fun getTargetedPhase(): TestPhase? {
        return testServices.moduleStructure.allDirectives[RUN_PIPELINE_TILL].firstOrNull() ?: defaultRunPipelineTill
    }

    private fun TestArtifactKind<*>.toPhase(): TestPhase? = when (this) {
        is FrontendKind -> TestPhase.FRONTEND
        is BackendKind -> TestPhase.FIR2IR
        is ArtifactKind -> TestPhase.BACKEND
        else -> null
    }

    private fun checkLatestPhaseDirective(): WrappedException? {
        val latestPhases = testServices.moduleStructure.allDirectives[LATEST_PHASE_IN_PIPELINE].distinct()
        val message = when (latestPhases.size) {
            0 -> "LATEST_PHASE_IN_PIPELINE directive is not specified for the test"
            1 -> return null
            else -> "LATEST_PHASE_IN_PIPELINE directive defined multiple times: $latestPhases"
        }
        return WrappedException.FromAfterAnalysisChecker(IllegalStateException(message))
    }

    private fun checkPhaseConsistency(): List<WrappedException> {
        val directives = testServices.moduleStructure.allDirectives
        if (DISABLE_NEXT_PHASE_SUGGESTION in directives) return emptyList()
        val expectedLastPhase = directives[LATEST_PHASE_IN_PIPELINE].first()
        val targetedPhase = getTargetedPhase()
        if (targetedPhase != null && targetedPhase > expectedLastPhase) {
            val message = "RUN_PIPELINE_TILL ($targetedPhase) cannot be greater than $LATEST_PHASE_IN_PIPELINE ($expectedLastPhase)"
            return listOf(WrappedException.FromAfterAnalysisChecker(IllegalStateException(message)))
        }

        return createDiffsForAllTestDataFiles("Phase $targetedPhase could be promoted to $expectedLastPhase") {
            val proposedDirectiveDeclaration = when (targetedPhase) {
                defaultRunPipelineTill -> ""
                else -> "// RUN_PIPELINE_TILL: $expectedLastPhase\n"
            }
            it.replace("// RUN_PIPELINE_TILL: $targetedPhase\n", proposedDirectiveDeclaration)
        }
    }

    private fun reportMissingDirective(failedAssertions: List<WrappedException>): List<WrappedException> {
        val latestPhases = testServices.moduleStructure.allDirectives[LATEST_PHASE_IN_PIPELINE]
        val expectedLastPhase = latestPhases.singleOrNull() ?: run {
            val message = when (latestPhases.size) {
                0 -> "LATEST_PHASE_IN_PIPELINE directive is not specified for the test"
                else -> "LATEST_PHASE_IN_PIPELINE directive defined multiple times: $latestPhases"
            }
            WrappedException.FromAfterAnalysisChecker(IllegalStateException(message))
        }
        val proposedPhase = failedAssertions.mapNotNull {
            when (it) {
                is WrappedException.FromFacade -> it.facade.outputKind
                is WrappedException.FromHandler if it.failureDisablesNextSteps -> it.handler.artifactKind
                else -> null
            }?.toPhase()
        }.minOrNull() ?: expectedLastPhase

        return createDiffsForAllTestDataFiles("Please specify the test phase in `// RUN_PIPELINE_TILL` directive") {
            @Suppress("ConvertToStringTemplate")
            "// RUN_PIPELINE_TILL: $proposedPhase\n" + it
        }
    }

    private fun createDiffsForAllTestDataFiles(
        message: String,
        newContent: (String) -> String
    ): List<WrappedException> {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val originalFile = testDataFile.originalTestDataFile
        val filesList = when {
            testDataFile.extension == "nkt" -> listOf(testDataFile)
            else -> listOf(
                originalFile,
                originalFile.firTestDataFile,
                originalFile.llFirTestDataFile,
                originalFile.latestLVTestDataFile,
                originalFile.reversedTestDataFile,
            )
        }
        return filesList.filter { it.exists() }.mapNotNull { file ->
            val contentWithNewDirective = newContent(file.readText())
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
        val hasNonSuppressibleFailuresFromFacade: Boolean,
    )

    private fun sortFailures(failedAssertions: List<WrappedException>): SortedFailures {
        val suppressibleFailures = mutableListOf<WrappedException>()
        val nonSuppressibleFailures = mutableListOf<WrappedException>()
        val targetedPhase = getTargetedPhase()!!
        var hasFailuresInNonLeafModule = false
        var hasNonSuppressibleFailuresFromFacade = false

        fun processFailure(module: TestModule?, kind: TestArtifactKind<*>, exception: WrappedException): MutableList<WrappedException> {
            val actualPhase = kind.toPhase()
            return when {
                module != null && !module.isLeafModule(testServices) -> {
                    hasFailuresInNonLeafModule = true
                    nonSuppressibleFailures
                }
                actualPhase == null -> nonSuppressibleFailures
                actualPhase == targetedPhase -> when {
                    exception is WrappedException.FromHandler && exception.failureDisablesNextSteps -> suppressibleFailures
                    exception is WrappedException.FromFacade -> {
                        hasNonSuppressibleFailuresFromFacade = true
                        nonSuppressibleFailures
                    }
                    else -> nonSuppressibleFailures
                }
                actualPhase > targetedPhase -> suppressibleFailures
                actualPhase < targetedPhase -> {
                    if (exception is WrappedException.FromFacade) {
                        hasNonSuppressibleFailuresFromFacade = true
                    }
                    nonSuppressibleFailures
                }
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
            hasFailuresInNonLeafModule,
            hasNonSuppressibleFailuresFromFacade
        )
    }
}
