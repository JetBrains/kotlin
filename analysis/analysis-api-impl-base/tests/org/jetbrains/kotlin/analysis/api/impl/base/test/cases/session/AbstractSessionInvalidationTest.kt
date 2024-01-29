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
import org.jetbrains.kotlin.test.services.moduleStructure

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

    override fun doTest(testServices: TestServices) {
        val allKtModules = testServices.ktTestModuleStructure.mainModules.map { it.ktModule }

        val sessionsBeforeModification = getSessions(allKtModules)
        testServices.ktTestModuleStructure.publishWildcardModificationEventsByDirective(modificationEventKind)
        val sessionsAfterModification = getSessions(allKtModules)

        val invalidatedSessions = buildSet {
            addAll(sessionsBeforeModification)
            removeAll(sessionsAfterModification)
        }

        checkInvalidatedModules(invalidatedSessions, testServices)
        checkSessionsMarkedInvalid(invalidatedSessions, testServices)
    }

    private fun getSessions(modules: List<KtModule>): List<SESSION> = modules.map(::getSession)

    private fun checkInvalidatedModules(
        invalidatedSessions: Set<SESSION>,
        testServices: TestServices,
    ) {
        val testModuleNames = testServices.moduleStructure.modules.map { it.name }

        val invalidatedModuleDescriptions = invalidatedSessions
            .map { getSessionKtModule(it).toString() }
            .filter {
                // We only want to include test modules in the output. Otherwise, it might include libraries from the test infrastructure.
                it in testModuleNames
            }
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

    private fun checkSessionsMarkedInvalid(
        invalidatedSessions: Set<SESSION>,
        testServices: TestServices,
    ) {
        invalidatedSessions.forEach { session ->
            testServices.assertions.assertFalse(isSessionValid(session)) {
                "The invalidated session `${session}` should have been marked invalid."
            }
        }
    }
}
