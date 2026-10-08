/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.handlers.updateTestDataIfNeeded
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.DISABLE_NEXT_PHASE_SUGGESTION
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.RUN_PIPELINE_TILL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class PhasedPipelineChecker(
    testServices: TestServices,
    val defaultRunPipelineTill: TestPhase? = null,
) : TestFailureSuppressor(testServices) {
    override val order: Order
        get() = Order.P4

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestPhaseDirectives)

    companion object {
        /**
         * If a file with this extension exists next to the test data file, the problem reported by [PhasedPipelineChecker]
         * is compared with its content instead of being reported (and the test data is never updated).
         * Exists for testing the checker itself.
         */
        const val PHASED_FAILURE_EXTENSION = ".phased-failure.txt"
    }

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        latestPhaseDirectiveOr { return failedAssertions + it.withFailFileCheck() }
        val targetedPhase = getTargetedPhase()
        if (targetedPhase == null) {
            return failedAssertions + reportMissingDirective(failedAssertions).withFailFileCheck()
        }

        val (suppressibleFailures, nonSuppressibleFailures, hasFailuresInNonLeafModule, hasNonSuppressibleFailuresFromFacade) = sortFailures(failedAssertions)

        val problem = when {
            suppressibleFailures.isEmpty() && !hasNonSuppressibleFailuresFromFacade && !hasFailuresInNonLeafModule -> checkPhaseConsistency()
            else -> null
        }
        return nonSuppressibleFailures + problem.withFailFileCheck()
    }

    /**
     * The test passed with no failures, but the phase directives still have to be checked,
     * e.g., the test might be promoted to a further phase.
     */
    override fun checkIfTestShouldBeUnmuted() {
        val failures = suppressIfNeeded(emptyList()).map { it.cause }
        updateTestDataIfNeeded(failures)
        testServices.assertions.failAll(failures)
    }

    private fun getTargetedPhase(): TestPhase? {
        return testServices.moduleStructure.allDirectives[RUN_PIPELINE_TILL].lastOrNull() ?: defaultRunPipelineTill
    }

    /**
     * Infers a test phase from its output artifact kind.
     */
    private fun TestArtifactKind<*>.toPhase(): TestPhase? = when (this) {
        is FrontendKind -> TestPhase.FRONTEND
        is BackendKind -> TestPhase.FIR2IR
        ArtifactKinds.Jvm -> TestPhase.CODEGEN
        ArtifactKinds.KLib -> TestPhase.CODEGEN
        else -> error("Cannot infer phase by output artifact kind `${this.javaClass.simpleName}`.")
    }

    private fun AbstractTestFacade<*, *>.toPhase(): TestPhase? =
        if (outputKind is BackendKind)
            when (inputKind) {
                is FrontendKind ->
                    TestPhase.FIR2IR
                is BackendKind -> {
                    require(this is IrPreSerializationLoweringFacade)
                    TestPhase.LOWERINGS
                }
                is ArtifactKinds.KLib -> {
                    require(this is DeserializerFacade)
                    TestPhase.CODEGEN
                }
                else -> error(
                    "Unexpected facade of type ${this.javaClass.simpleName} taking input artifact of kind=$this, " +
                            "producing output artifact of kind=${outputKind.javaClass.simpleName}"
                )
            }
        else
            outputKind.toPhase()

    private fun AnalysisHandler<*>.toPhase(): TestPhase? {
        // TODO KT-78539: Different phases must be derived for handlers of different handler steps, all having artifactKind=IrBackend
        return artifactKind.toPhase()
    }

    private inline fun latestPhaseDirectiveOr(otherwise: (Problem) -> Nothing): TestPhase {
        val latestPhases = testServices.moduleStructure.allDirectives[LATEST_PHASE_IN_PIPELINE].distinct()
        val message = when (latestPhases.size) {
            1 -> return latestPhases[0]
            0 -> "LATEST_PHASE_IN_PIPELINE directive is not specified for the test"
            else -> "LATEST_PHASE_IN_PIPELINE directive defined multiple times: $latestPhases"
        }
        otherwise(Problem(message))
    }

    private fun checkPhaseConsistency(): Problem? {
        val directives = testServices.moduleStructure.allDirectives
        if (DISABLE_NEXT_PHASE_SUGGESTION in directives) return null
        val expectedLastPhase = directives[LATEST_PHASE_IN_PIPELINE].first()
        val targetedPhase = getTargetedPhase()
        if (targetedPhase == expectedLastPhase) {
            return null
        }
        if (targetedPhase != null && targetedPhase > expectedLastPhase) {
            return Problem("RUN_PIPELINE_TILL ($targetedPhase) cannot be greater than $LATEST_PHASE_IN_PIPELINE ($expectedLastPhase)")
        }

        return Problem("Phase $targetedPhase could be promoted to $expectedLastPhase") {
            val proposedDirectiveDeclaration = when (targetedPhase) {
                defaultRunPipelineTill -> ""
                else -> "// RUN_PIPELINE_TILL: $expectedLastPhase"
            }
            it.replace("// RUN_PIPELINE_TILL: $targetedPhase", proposedDirectiveDeclaration)
        }
    }

    private fun reportMissingDirective(failedAssertions: List<WrappedException>): Problem {
        val expectedLastPhase = latestPhaseDirectiveOr { return it }
        val proposedPhase = failedAssertions.mapNotNull {
            when (it) {
                is WrappedException.FromFacade -> it.facade.outputKind
                is WrappedException.FromHandler if it.failureDisablesNextSteps -> it.handler.artifactKind
                else -> null
            }?.toPhase()
        }.minOrNull() ?: expectedLastPhase

        return Problem("Please specify the test phase in `// RUN_PIPELINE_TILL` directive, e.g. `// RUN_PIPELINE_TILL: $proposedPhase`") {
            @Suppress("ConvertToStringTemplate")
            "// RUN_PIPELINE_TILL: $proposedPhase\n" + it
        }
    }

    /**
     * @param newContent if not null, the problem is reported as a difference between the current test data and the
     * one transformed by [newContent]; otherwise, as a plain error.
     */
    private class Problem(val message: String, val newContent: ((String) -> String)? = null)

    private fun Problem?.withFailFileCheck(): List<WrappedException> {
        val failFile = testServices.moduleStructure.originalTestDataFiles.first().originalTestDataFile
            .withExtension(PHASED_FAILURE_EXTENSION)

        return when {
            failFile.exists() -> when (this) {
                null -> listOf(IllegalStateException("There's no error from ${PhasedPipelineChecker::class.simpleName}. Please remove `${failFile.name}`").wrap())
                else -> try {
                    testServices.assertions.assertEqualsToFile(failFile, message)
                    emptyList()
                } catch (e: AssertionError) {
                    listOf(e.wrap())
                }
            }
            this == null -> emptyList()
            newContent == null -> listOf(IllegalStateException(message).wrap())
            else -> createDiffsForAllTestDataFiles(message, newContent)
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
                originalFile.llFirTestDataFile,
                originalFile.latestLVTestDataFile,
                originalFile.reversedTestDataFile,
                originalFile.partialBodyTestDataFile,
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

        fun processFailure(module: TestModule?, actualPhase: TestPhase?, exception: WrappedException): MutableList<WrappedException> {
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
                is WrappedException.FromTestPipeline -> nonSuppressibleFailures
                is WrappedException.FromFacade ->
                    processFailure(exception.failedModule, exception.facade.toPhase(), exception)
                is WrappedException.WrappedExceptionWithoutModule -> nonSuppressibleFailures
                is WrappedException.FromHandler ->
                    processFailure(exception.failedModule, exception.phase ?: exception.handler.toPhase(), exception)
                is WrappedException.FromGroupingFacade,
                is WrappedException.FromGroupingHandler ->
                    processFailure(module = null, TestPhase.CODEGEN, exception)
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
