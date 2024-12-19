/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.TestTierDirectives
import org.jetbrains.kotlin.test.directives.TestTierDirectives.DISABLE_NEXT_TIER_SUGGESTION
import org.jetbrains.kotlin.test.directives.TestTierDirectives.RUN_PIPELINE_TILL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.partitionNotNull
import org.jetbrains.kotlin.utils.joinToEnglishAndString
import org.jetbrains.kotlin.utils.joinToEnglishOrString
import org.opentest4j.AssertionFailedError
import java.io.File
import kotlin.collections.plus

/**
 * The list of all possible test tier labels one can use in
 * [RUN_PIPELINE_TILL][org.jetbrains.kotlin.test.directives.TestTierDirectives.RUN_PIPELINE_TILL].
 * Prefer obtaining [TestTier] though, as some tiers may not be applicable for some tests.
 */
enum class TestTierLabel {
    FRONTEND,
    FIR2IR,
    BACKEND;

    fun toTier(list: List<TestTier>): TestTier = TestTier(this, list)
}

class TierPassesMarker(val tier: TestTier, val module: TestModule, val origin: AnalysisHandler<*>) : Exception() {
    override val message: String
        get() = "Looks like tier $tier passes with no error diagnostics. Please update the tier directive to `// $RUN_PIPELINE_TILL: ${tier.next}`${
            suggestSubsequentTiersIfAppropriate(tier.next)
        } and regenerate tests."
}

private fun TestTier.allSubsequent(): List<TestTier> = when {
    isLast -> emptyList()
    else -> generateSequence(next) { if (!it.isLast) it.next else null }.toList()
}

private fun suggestSubsequentTiersIfAppropriate(tier: TestTier): String =
    when {
        tier.isLast -> ""
        else -> " (maybe even ${tier.allSubsequent().joinToEnglishOrString()} if you are sure)"
    }

class TestTier(
    val label: TestTierLabel,
    /**
     * Encapsulates the logic of obtaining the [next] tier into the current tier itself.
     */
    private val container: List<TestTier>,
) {
    val name get() = label.name
    override fun toString(): String = name

    val next: TestTier get() = container[container.indexOf(this) + 1]

    val isLast: Boolean get() = container.last() == this
}

fun Collection<TestTierLabel>.toTiers(): List<TestTier> =
    buildList {
        for (label in this@toTiers) {
            add(label.toTier(this))
        }
    }

val TestModuleStructure.applicableTestTiers: List<TestTier>
    get() = TestTierLabel.entries.toTiers()

/**
 * Checks consistency of [tier][TestTierLabel]-related directives in tests that are not generated into any specific tier runner.
 */
open class PartialTestTierChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    companion object {
        const val UPDATE_TIERS_AUTOMATICALLY_PROPERTY_NAME = "kotlin.test.update-tiers-automatically"
        val UPDATE_TIERS_AUTOMATICALLY = System.getProperty(UPDATE_TIERS_AUTOMATICALLY_PROPERTY_NAME, "true") == "true"

        const val TIERED_FAILURE_EXTENSION = ".tiered-failure.txt"
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestTierDirectives)

    override fun check(failedAssertions: List<WrappedException>) {}

    protected fun checkDirectivesBasicConsistency(): TieredHandlerException? {
        val applicableTiers = testServices.moduleStructure.applicableTestTiers
        val applicableTierLabels = applicableTiers.map { it.label }
        val disableText = testServices.moduleStructure.allDirectives[DISABLE_NEXT_TIER_SUGGESTION]
        val declaredTier = testServices.moduleStructure.allDirectives[RUN_PIPELINE_TILL].firstOrNull()

        if (!isDirectiveSetInTest) {
            setTierIfNeeded(applicableTierLabels.first())
            return "There's no `// $RUN_PIPELINE_TILL` directive in the test. Please set an appropriate tier and regenerate tests"
                .let(::TieredHandlerException)
        }

        if (DISABLE_NEXT_TIER_SUGGESTION in testServices.moduleStructure.allDirectives && disableText.isEmpty()) {
            return "Please specify the reason why the tier upgrade suggestion is disabled in `// $DISABLE_NEXT_TIER_SUGGESTION`"
                .let(::TieredHandlerException)
        }

        if (declaredTier != null && declaredTier !in applicableTierLabels) {
            setTierIfNeeded(applicableTierLabels.first())
            return "The test declares the tier $declaredTier, but is configured to only run ${applicableTiers.joinToEnglishAndString()}. Please set an appropriate tier and regenerate tests"
                .let(::TieredHandlerException)
        }

        return null
    }

    protected fun TieredHandlerException.withFailFileDump(): WrappedException.FromAfterAnalysisChecker? {
        val ownException = this
        val originalFile = testServices.moduleStructure.modules.first().files.first().originalFile
        val failFile = originalFile.withExtension(TIERED_FAILURE_EXTENSION)

        return when {
            !failFile.exists() -> WrappedException.FromAfterAnalysisChecker(ownException)
            else -> try {
                testServices.assertions.assertEqualsToFile(failFile, ownException.message, sanitizer = { it.replace("\r", "").trim() })
                null
            } catch (a: AssertionFailedError) {
                WrappedException.FromAfterAnalysisChecker(a)
            }
        }
    }

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> =
        failedAssertions + checkDirectivesBasicConsistency()?.withFailFileDump().let(::listOfNotNull)

    protected fun setTierIfNeeded(label: TestTierLabel) {
        if (!UPDATE_TIERS_AUTOMATICALLY) {
            return
        }

        fun File.writeDirective() {
            if (exists()) {
                if (featuresDirective(RUN_PIPELINE_TILL.name)) {
                    writeText(readText().replace("// $RUN_PIPELINE_TILL: .*".toRegex(), "// $RUN_PIPELINE_TILL: $label"))
                } else {
                    writeText("// $RUN_PIPELINE_TILL: $label\n" + readText())
                }
            }
        }

        val originalFile = testServices.moduleStructure.modules.first().files.first().originalFile.originalOrNktTestDataFile
        val failFile = originalFile.withExtension(TIERED_FAILURE_EXTENSION)

        if (failFile.exists()) {
            return
        }

        listOf(
            originalFile,
            originalFile.firTestDataFile,
            originalFile.llFirTestDataFile,
            originalFile.latestLVTestDataFile,
            originalFile.reversedTestDataFile,
        ).forEach(File::writeDirective)
    }

    private val isDirectiveSetInTest: Boolean
        get() {
            val originalFile = testServices.moduleStructure.modules.first().files.first().originalFile.originalOrNktTestDataFile
            return originalFile.featuresDirective(RUN_PIPELINE_TILL.name)
        }

    private val File.originalOrNktTestDataFile: File
        get() {
            val baseFile = testServices.moduleStructure.modules.first().files.first().originalFile
            return baseFile.takeIf { it.path.endsWith(".nkt") } ?: baseFile.originalTestDataFile
        }

    private fun File.featuresDirective(directive: String): Boolean =
        readLines().any { it.matches("""^// $directive\b.*""".toRegex()) }

    protected class TieredHandlerException(override val message: String) : RuntimeException(message)
}

/**
 * Checks consistency of [tier][TestTierLabel]-related directives.
 * Works together with [testTierExceptionInverter][org.jetbrains.kotlin.test.backend.handlers.testTierExceptionInverter]s,
 * which throw "marker" exceptions.
 * Markers signify that the related tier handler passes without throwing and are only emitted for
 * the same tier as defined by [lastTierCurrentPipelineExecutes] - markers of lower tiers are simply not needed.
 * A tier may require zero or arbitrarily many markers.
 */
class TestTierChecker(
    private val lastTierCurrentPipelineExecutes: TestTierLabel,
    private val numberOfMarkerHandlersPerModule: Int,
    testServices: TestServices,
) : PartialTestTierChecker(testServices) {
    private fun checkDirectivesFullConsistency(
        successfulTierMarkers: List<TierPassesMarker>,
        thereAreOtherFailures: Boolean,
    ): TieredHandlerException? {
        checkDirectivesBasicConsistency()?.let { return it }

        val applicableTiers = testServices.moduleStructure.applicableTestTiers
        val applicableTierLabels = applicableTiers.map { it.label }
        val declaredTier = testServices.moduleStructure.allDirectives[RUN_PIPELINE_TILL].firstOrNull()

        if (successfulTierMarkers.any { it.tier.label != lastTierCurrentPipelineExecutes }) {
            return "Markers of tiers other than $lastTierCurrentPipelineExecutes should not be thrown when using a $lastTierCurrentPipelineExecutes runner. The test runner probably contains some redundant early-tier handlers"
                .let(::TieredHandlerException)
        }

        if (lastTierCurrentPipelineExecutes !in applicableTierLabels) {
            throw "This is a $lastTierCurrentPipelineExecutes test runner, it's not applicable for tests configured to run ${applicableTiers.joinToEnglishAndString()}. Please regenerate tests"
                .let(::TieredHandlerException)
        }

        val moduleToSuccessMarkers = successfulTierMarkers
            .groupingBy { it.module }
            .aggregate<TierPassesMarker, TestModule, MutableSet<AnalysisHandler<*>>> { _, acc, element, _ ->
                acc?.apply { add(element.origin) } ?: mutableSetOf(element.origin)
            }

        val everyModuleIsSuccessful = testServices.moduleStructure.modules.all {
            moduleToSuccessMarkers[it]?.size == numberOfMarkerHandlersPerModule
        }

        if (declaredTier != null && lastTierCurrentPipelineExecutes < declaredTier) {
            return "A $lastTierCurrentPipelineExecutes runner is not enough for a $declaredTier test. Please regenerate tests"
                .let(::TieredHandlerException)
        }

        if (everyModuleIsSuccessful && !thereAreOtherFailures) {
            if (DISABLE_NEXT_TIER_SUGGESTION in testServices.moduleStructure.allDirectives) {
                return null
            }

            setTierIfNeeded(successfulTierMarkers.first().tier.next.label)
            return successfulTierMarkers.first().message
                .let(::TieredHandlerException)
        }

        return null
    }

    private fun TieredHandlerException?.withFailFileFullCheck(): WrappedException.FromAfterAnalysisChecker? {
        val originalFile = testServices.moduleStructure.modules.first().files.first().originalFile
        val failFile = originalFile.withExtension(TIERED_FAILURE_EXTENSION)

        return when {
            this != null -> withFailFileDump()
            failFile.exists() -> "There's no error from ${this@TestTierChecker::class.simpleName}, removing the `$TIERED_FAILURE_EXTENSION` file. Please rerun the test"
                .let(::Exception)
                .let(WrappedException::FromAfterAnalysisChecker)
                .also { failFile.delete() }
            else -> null
        }
    }

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val (successfulTierMarkers, otherFailures) = failedAssertions.partitionNotNull {
            (it as? WrappedException.FromHandler)?.cause as? TierPassesMarker
        }

        return otherFailures + checkDirectivesFullConsistency(successfulTierMarkers, otherFailures.isNotEmpty())
            .withFailFileFullCheck().let(::listOfNotNull)
    }
}
