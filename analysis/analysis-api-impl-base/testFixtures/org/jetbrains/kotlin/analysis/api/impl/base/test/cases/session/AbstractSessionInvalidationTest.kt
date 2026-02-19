/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventKind
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.directives.publishWildcardModificationEventsByDirective
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Checks that sessions are invalidated after publishing modification events. The type of published modification event depends on the value
 * of [modificationEventKind]. This allows [AbstractSessionInvalidationTest] to check all modification event kinds with the same original
 * test data.
 *
 * [AbstractSessionInvalidationTest] is a base class for invalidation tests of `KaSession` and `LLFirSession`, which share the test
 * data but not the output data (see also [testOutputSubdirectoryName]).
 *
 * @param S The type of the session, either an LL FIR or an analysis session.
 */
abstract class AbstractSessionInvalidationTest<S> : AbstractAnalysisApiBasedTest() {
    /**
     * The kind of modification event to be published for the invalidation. Each modification event is tested separately and has its own
     * associated result file.
     */
    protected abstract val modificationEventKind: KotlinModificationEventKind

    /**
     * A directory name for the test output files to distinguish them from the output files of other session invalidation tests. The
     * specified directory is a subdirectory of the test directory path.
     */
    protected abstract val testOutputSubdirectoryName: String

    protected abstract fun getSessions(ktTestModule: KtTestModule): List<TestSession<S>>

    /**
     * In some cases, it might be legal for a session cache to evict sessions which are still valid. Such sessions would fail the validity
     * check (see [checkSessionsMarkedInvalid]) and should be skipped.
     */
    protected open fun shouldSkipValidityCheck(session: TestSession<S>): Boolean = false

    override fun doTest(testServices: TestServices) {
        val ktTestModules = testServices.ktTestModuleStructure.mainModules

        val sessionsBeforeModification = getAllSessions(ktTestModules)
        ensureLibraryFallbackDependenciesSessionsExist(ktTestModules)
        checkSessionValidityBeforeModification(sessionsBeforeModification, testServices)

        testServices.ktTestModuleStructure.publishWildcardModificationEventsByDirective(modificationEventKind)
        val sessionsAfterModification = getAllSessions(ktTestModules)

        val invalidatedSessions = buildSet {
            addAll(sessionsBeforeModification)
            removeAll(sessionsAfterModification)
        }

        checkInvalidatedModules(invalidatedSessions, testServices)
        checkSessionsMarkedInvalid(invalidatedSessions, testServices)

        val untouchedSessions = sessionsBeforeModification.intersect(sessionsAfterModification)
        checkUntouchedSessionValidity(untouchedSessions, testServices)
    }

    private fun getAllSessions(modules: List<KtTestModule>): List<TestSession<S>> = modules.flatMap(::getSessions)

    /**
     * We have to ensure manually that fallback dependencies sessions exist so that they can be properly tested for invalidation. This is
     * because [KaLibraryFallbackDependenciesModule]s aren't materialized as test modules (see [AnalysisApiTestDirectives.FALLBACK_DEPENDENCIES][org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives.FALLBACK_DEPENDENCIES]),
     * and [getAllSessions] only creates sessions for test modules. So unless we grab the session explicitly, it might not be created on its
     * own.
     *
     * This is also relevant for analysis session invalidation because it depends on LL FIR session invalidation.
     */
    private fun ensureLibraryFallbackDependenciesSessionsExist(ktTestModules: List<KtTestModule>) {
        ktTestModules.forEach { ktTestModule ->
            val kaModule = ktTestModule.ktModule
            if (kaModule.directRegularDependencies.none { it is KaLibraryFallbackDependenciesModule }) {
                return@forEach
            }

            // This triggers dependency session creation through symbol providers without depending on `low-level-api-fir`.
            analyze(kaModule) {
                findClass(ClassId(FqName.ROOT, Name.identifier("IDontExistAtAll")))
            }
        }
    }

    private fun checkInvalidatedModules(
        invalidatedSessions: Set<TestSession<S>>,
        testServices: TestServices,
    ) {
        val invalidatedSessionDescriptions = invalidatedSessions
            .map { it.description }
            .distinct()
            .sorted()

        val actualText = buildString {
            appendLine("Invalidated sessions:")
            invalidatedSessionDescriptions.forEach { appendLine(it) }
        }

        testServices.assertions.assertEqualsToTestOutputFile(
            actualText,
            extension = ".${modificationEventKind.name.lowercase()}.txt",
            subdirectoryName = testOutputSubdirectoryName,
        )
    }

    private fun checkSessionValidityBeforeModification(
        sessions: List<TestSession<S>>,
        testServices: TestServices,
    ) {
        sessions.forEach { session ->
            testServices.assertions.assertTrue(session.isValid) {
                "The session `$session` should be valid before invalidation is triggered."
            }
        }
    }

    private fun checkSessionsMarkedInvalid(
        invalidatedSessions: Set<TestSession<S>>,
        testServices: TestServices,
    ) {
        invalidatedSessions.forEach { session ->
            if (shouldSkipValidityCheck(session)) return@forEach

            testServices.assertions.assertFalse(session.isValid) {
                "The invalidated session `${session}` should have been marked invalid."
            }
        }
    }

    private fun checkUntouchedSessionValidity(
        sessions: Set<TestSession<S>>,
        testServices: TestServices,
    ) {
        sessions.forEach { session ->
            testServices.assertions.assertTrue(session.isValid) {
                "The session `$session` has not been evicted from the session cache and should still be valid."
            }
        }
    }

    companion object {
        val TEST_OUTPUT_DIRECTORY_NAMES = listOf("firSession", "analysisSession")
    }
}
