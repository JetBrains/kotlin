/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.handlers.JvmBoxRunner
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.collectTestModulesByName
import org.jetbrains.kotlin.test.frontend.fir.shouldRunFirFrontendFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.partitionNotNull
import org.jetbrains.kotlin.utils.addToStdlib.previous
import org.opentest4j.AssertionFailedError
import java.io.File

/**
 * See the description of
 * [RUN_PIPELINE_TILL][org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.RUN_PIPELINE_TILL].
 */
enum class TestTiers {
    SOURCE,
    FIR,
    FIR2IR,
    KLIB,
    BACKEND,
    BOX,
}

fun WrappedException.guessTierOfFailure(): TestTiers? {
    val tierArtifactKind = when (this) {
        is WrappedException.FromFacade -> facade.outputKind
        is WrappedException.FromHandler -> when {
            handler is JvmBoxRunner -> return TestTiers.BOX
            else -> handler.artifactKind
        }
        else -> return null
    }

    return when (tierArtifactKind) {
        FrontendKinds.FIR -> TestTiers.FIR
        BackendKinds.IrBackend -> TestTiers.FIR2IR
        BackendKinds.IrBackendForK1AndK2 -> TestTiers.FIR2IR
        ArtifactKinds.KLib -> TestTiers.KLIB
        ArtifactKinds.Jvm -> TestTiers.BACKEND
        ArtifactKinds.JvmFromK1AndK2 -> TestTiers.BACKEND
        ArtifactKinds.Js -> TestTiers.BACKEND
        ArtifactKinds.Native -> TestTiers.BACKEND
        ArtifactKinds.Wasm -> TestTiers.BACKEND
        else -> TestTiers.BACKEND
    }
}

private val IGNORE_BACKEND_DIRECTIVES = listOf(
    IGNORE_BACKEND,
    IGNORE_BACKEND_K2,
    IGNORE_BACKEND_MULTI_MODULE,
    IGNORE_BACKEND_K2_MULTI_MODULE,
)

class TestTierChecker(
    private val lastTierCurrentPipelineExecutes: TestTiers,
    testServices: TestServices,
) : AfterAnalysisChecker(testServices) {
    override fun check(failedAssertions: List<WrappedException>) {}

    private fun analyzeFailures(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (FirDiagnosticsDirectives.RUN_PIPELINE_TILL !in testServices.moduleStructure.allDirectives) {
            return when (lastTierCurrentPipelineExecutes) {
                TestTiers.entries.last() -> setAppropriateDirective(failedAssertions)
                else -> failedAssertions
            }
        }

        val declaredTier = testServices.moduleStructure.allDirectives[FirDiagnosticsDirectives.RUN_PIPELINE_TILL]
            .first().let(TestTiers::valueOf)

        val latestMarkedTier = testServices.moduleStructure.modules.maxOf { it.latestExpectedTier }

        val (trackedFailures, ignoredFailures) = failedAssertions.partition {
            val tierOfFailure = it.guessTierOfFailure() ?: return@partition true
            tierOfFailure <= declaredTier
        }

        if (trackedFailures.isNotEmpty()) {
            return trackedFailures
        }

        fun requestTierUpgrade(to: TestTiers): Nothing =
            throw "Looks like some tiers above $declaredTier pass with no exceptions. Please update the tier directive to `// ${FirDiagnosticsDirectives.RUN_PIPELINE_TILL}: ${to.name}` and regenerate tests."
                .let(::TieredHandlerException)

        if (ignoredFailures.isEmpty()) {
            val possibleTierUpgrade = minOf(latestMarkedTier, lastTierCurrentPipelineExecutes)

            if (declaredTier < possibleTierUpgrade) {
                requestTierUpgrade(possibleTierUpgrade)
            }

            if (latestMarkedTier == declaredTier && latestMarkedTier < lastTierCurrentPipelineExecutes) {
                return emptyList()
            }

            if (latestMarkedTier < declaredTier) {
                throw "The test declares the tier $declaredTier, but is configured to only run up to $latestMarkedTier. Please set `// ${FirDiagnosticsDirectives.RUN_PIPELINE_TILL}: ${latestMarkedTier}` and regenerate tests as we can't check later tiers for it"
                    .let(::TieredHandlerException)
            }

            if (lastTierCurrentPipelineExecutes == TestTiers.entries.last()) {
                return emptyList()
            }

            throw "Please regenerate tests, this test runner can only run up to $lastTierCurrentPipelineExecutes (including), but the test seems to successfully pass all these tiers."
                .let(::TieredHandlerException)
        }

        // `ignoredFailures` can never contain the first tier
        val lowestIgnoredTier = ignoredFailures.mapNotNull { it.guessTierOfFailure() }.min()
        val suggestedDeclaredTier = lowestIgnoredTier.previous

        if (lastTierCurrentPipelineExecutes < suggestedDeclaredTier) {
            throw "Test infrastructure misconfiguration: this `TestTierChecker` declares `lastTierCurrentPipelineExecutes = $lastTierCurrentPipelineExecutes`, but we've just caught an error from a higher tier ($suggestedDeclaredTier)"
                .let(::TieredHandlerException)
        }

        return when (declaredTier) {
            suggestedDeclaredTier -> emptyList()
            else -> requestTierUpgrade(suggestedDeclaredTier)
        }
    }

    private fun setAppropriateDirective(failedAssertions: List<WrappedException>): List<WrappedException> {
        val (failureTiers, otherFailures) = failedAssertions.partitionNotNull {
            it.guessTierOfFailure()
        }

        if (otherFailures.isNotEmpty()) {
            return otherFailures
        }

        val latestMarkedTier = testServices.moduleStructure.modules.maxOf { it.latestExpectedTier }

        val suggestedTier = when {
            failureTiers.isEmpty() -> latestMarkedTier
            else -> minOf(failureTiers.min().previous, latestMarkedTier)
        }

        setTier(suggestedTier)
        return emptyList()
    }

    private fun setTier(tier: TestTiers) {
        val originalFile = testServices.moduleStructure.modules.first().files.first().originalFile.originalTestDataFile
        originalFile.writeText("// RUN_PIPELINE_TILL: $tier\n" + originalFile.readText())

        fun File.writeDirective() {
            if (exists()) {
                writeText("// RUN_PIPELINE_TILL: $tier\n" + readText())
            }
        }

        listOf(
            originalFile.firTestDataFile,
            originalFile.llFirTestDataFile,
            originalFile.latestLVTestDataFile,
            originalFile.reversedTestDataFile,
        ).forEach(File::writeDirective)
    }

    private val testModulesByName by lazy { testServices.collectTestModulesByName() }

    /**
     * The last tier this module is expected to be compiled up to by its nature.
     * For example, if this is a common module, it should only be compiled up to
     * [TestTiers.KLIB], and if this is a JVM module - up to [TestTiers.BOX].
     */
    private val TestModule.latestExpectedTier: TestTiers
        get() = when {
            // Sometimes we don't run FIR for common modules.
            // See: `compiler/testData/diagnostics/testsWithStdLib/multiplatform/actualExternalInJs.kt`
            !shouldRunFirFrontendFacade(this, testServices, testModulesByName) -> TestTiers.SOURCE
            IGNORE_BACKEND_DIRECTIVES.any { directive -> targetBackend in directives[directive] } -> TestTiers.KLIB
            targetPlatform.isJvm() -> TestTiers.BOX
            else -> TestTiers.KLIB
        }

    private class TieredHandlerException(override val message: String) : Exception(message)

    companion object {
        const val TIERED_FAILURE_EXTENSION = ".tiered-failure.txt"
    }

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        return try {
            analyzeFailures(failedAssertions) + preserveFailuresThatMustBeCaughtByLaterCheckers(failedAssertions)
        } catch (e: TieredHandlerException) {
            val originalFile = testServices.moduleStructure.modules.first().files.first().originalFile
            val failFile = File(originalFile.path.removeSuffix(".kt") + TIERED_FAILURE_EXTENSION)

            if (!failFile.exists()) {
                return WrappedException.FromAfterAnalysisChecker(e).let(::listOf)
            }

            try {
                testServices.assertions.assertEqualsToFile(failFile, e.message, sanitizer = { it.replace("\r", "").trim() })
            } catch (a: AssertionFailedError) {
                return WrappedException.FromAfterAnalysisChecker(a).let(::listOf)
            }

            emptyList()
        }
    }

    /**
     * It may be the case that K2 fails with an exception (e.g., StackOverflow),
     * and there's a `.fir.fail` file, so it must be suppressed, but the suppressor
     * sits later in the `afterAnalysisCheckers` list than [TestTierChecker], because
     * it was added via [forTestsMatching][org.jetbrains.kotlin.test.builders.TestConfigurationBuilder.forTestsMatching].
     * If the suppressor had come before us, we wouldn't have seen this failure in this list.
     *
     * See: [org.jetbrains.kotlin.test.runners.TieredFirJvmLightTreeTestGenerated.Tests.Multiplatform.Hmpp.testRecursiveActualTypealiasExpansion_2].
     */
    private fun preserveFailuresThatMustBeCaughtByLaterCheckers(failedAssertions: List<WrappedException>): List<WrappedException> {
        return failedAssertions.filter {
            it is WrappedException.FromFacade && it.facade is FirFrontendFacade && firFailFileExists
        }
    }

    private val firFailFileExists: Boolean
        get() {
            val testFile = testServices.moduleStructure.originalTestDataFiles.first().firTestDataFile
            return testFile.parentFile.resolve("${testFile.nameWithoutExtension}.fail").exists()
        }
}
