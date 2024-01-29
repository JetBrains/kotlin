/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.directives.ModificationEventKind
import org.jetbrains.kotlin.analysis.test.framework.directives.publishWildcardModificationEventsByDirective
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Checks that sessions are invalidated after publishing modification events. The type of published modification event depends on the value
 * of [modificationEventKind]. This allows [AbstractSessionInvalidationTest] to check all modification event kinds with the same original
 * test data.
 *
 * [AbstractSessionInvalidationTest] is a base class for invalidation tests of `KtAnalysisSession` and `LLFirSession`, which share the test
 * data but not necessarily the result data (see also [resultFileSuffix]).
 */
abstract class AbstractSessionInvalidationTest<SESSION> : AbstractAnalysisApiBasedTest() {
    /**
     * The kind of modification event to be published for the invalidation. Each modification event is tested separately and has its own
     * associated result file.
     */
    protected abstract val modificationEventKind: ModificationEventKind

    /**
     * A suffix for the result file to distinguish it from the results of other session invalidation tests if the results are different.
     */
    protected abstract val resultFileSuffix: String?

    protected abstract fun getSession(ktModule: KtModule): SESSION

    protected abstract fun getSessionKtModule(session: SESSION): KtModule

    protected abstract fun isSessionValid(session: SESSION): Boolean

    /**
     * In some cases, it might be legal for a session cache to evict sessions which are still valid. Such sessions would fail the validity
     * check (see [checkSessionsMarkedInvalid]) and should be skipped.
     */
    protected open fun shouldSkipValidityCheck(session: SESSION): Boolean = false

    override fun doTest(testServices: TestServices) {
        val ktModules = testServices.ktTestModuleStructure.mainModules.map { it.ktModule }

        val sessionsBeforeModification = getSessions(ktModules)
        checkSessionValidityBeforeModification(sessionsBeforeModification, testServices)

        testServices.ktTestModuleStructure.publishWildcardModificationEventsByDirective(modificationEventKind)
        val sessionsAfterModification = getSessions(ktModules)

        val invalidatedSessions = buildSet {
            addAll(sessionsBeforeModification)
            removeAll(sessionsAfterModification)
        }

        checkInvalidatedModules(invalidatedSessions, testServices)
        checkSessionsMarkedInvalid(invalidatedSessions, testServices)

        val untouchedSessions = sessionsBeforeModification.intersect(sessionsAfterModification)
        checkUntouchedSessionValidity(untouchedSessions, testServices)
    }

    private fun getSessions(modules: List<KtModule>): List<SESSION> = modules.map(::getSession)

    private fun checkInvalidatedModules(
        invalidatedSessions: Set<SESSION>,
        testServices: TestServices,
    ) {
        val invalidatedModuleDescriptions = invalidatedSessions
            .map { getSessionKtModule(it).toString() }
            .distinct()
            .sorted()

        val actualText = buildString {
            appendLine("Module names of invalidated sessions:")
            invalidatedModuleDescriptions.forEach { appendLine(it) }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(
            actualText,
            extension = ".${modificationEventKind.name.lowercase()}.txt",

            // Support differing result data. Using `testPrefix` takes away the ability for different kinds of tests (such as IDE vs.
            // Standalone modes) to have different test results (since `testPrefix` normally supports this functionality), but (1) we are
            // currently only testing the IDE mode and (2) the test results between different modes should not differ for session
            // invalidation in the first place.
            testPrefix = resultFileSuffix,
        )
    }

    private fun checkSessionValidityBeforeModification(
        sessions: List<SESSION>,
        testServices: TestServices,
    ) {
        sessions.forEach { session ->
            testServices.assertions.assertTrue(isSessionValid(session)) {
                "The session `$session` should be valid before invalidation is triggered."
            }
        }
    }

    private fun checkSessionsMarkedInvalid(
        invalidatedSessions: Set<SESSION>,
        testServices: TestServices,
    ) {
        invalidatedSessions.forEach { session ->
            if (shouldSkipValidityCheck(session)) return@forEach

            testServices.assertions.assertFalse(isSessionValid(session)) {
                "The invalidated session `${session}` should have been marked invalid."
            }
        }
    }

    private fun checkUntouchedSessionValidity(
        sessions: Set<SESSION>,
        testServices: TestServices,
    ) {
        sessions.forEach { session ->
            testServices.assertions.assertTrue(isSessionValid(session)) {
                "The session `$session` has not been evicted from the session cache and should still be valid."
            }
        }
    }
}
