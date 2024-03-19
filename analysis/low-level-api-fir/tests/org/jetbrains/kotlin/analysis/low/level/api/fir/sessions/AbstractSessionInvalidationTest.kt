/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.directives.ModificationEventKind
import org.jetbrains.kotlin.analysis.test.framework.directives.publishWildcardModificationEventsByDirective
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Checks that sessions are invalidated after publishing modification events. The type of published modification event depends on the value
 * of [modificationEventKind]. This allows [AbstractSessionInvalidationTest] to check all modification event kinds with the same original
 * test data.
 *
 * See also `AbstractSessionInvalidationTest` on the IDE side, which is still required to check session invalidation in module structures
 * with cyclic dependencies.
 */
abstract class AbstractSessionInvalidationTest : AbstractAnalysisApiBasedTest() {
    protected abstract val modificationEventKind: ModificationEventKind

    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val allKtModules = testServices.ktTestModuleStructure.mainModules.map { it.ktModule }

        val sessionsBeforeModification = getSessionsFor(allKtModules)
        testServices.ktTestModuleStructure.publishWildcardModificationEventsByDirective(modificationEventKind)
        val sessionsAfterModification = getSessionsFor(allKtModules)

        val invalidatedSessions = buildSet {
            addAll(sessionsBeforeModification)
            removeAll(sessionsAfterModification)
        }

        checkInvalidatedModules(moduleStructure, invalidatedSessions, testServices)
        checkSessionsMarkedInvalid(invalidatedSessions, testServices)
    }

    private fun getSessionsFor(modules: List<KtModule>): List<LLFirSession> {
        val project = modules.first().project
        val sessionCache = LLFirSessionCache.getInstance(project)
        return modules.map { sessionCache.getSession(it) }
    }

    private fun checkInvalidatedModules(
        testModuleStructure: TestModuleStructure,
        invalidatedSessions: Set<LLFirSession>,
        testServices: TestServices,
    ) {
        val testModuleNames = testModuleStructure.modules.map { it.name }

        val invalidatedModuleDescriptions = invalidatedSessions
            .map { it.ktModule.toString() }
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

        testServices.assertions.assertEqualsToTestDataFileSibling(actualText, extension = ".${modificationEventKind.name.lowercase()}.txt")
    }

    private fun checkSessionsMarkedInvalid(
        invalidatedSessions: Set<LLFirSession>,
        testServices: TestServices,
    ) {
        invalidatedSessions.forEach { session ->
            testServices.assertions.assertFalse(session.isValid) {
                "The invalidated session `${session}` should have been marked invalid."
            }
        }
    }

    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractModuleStateModificationSessionInvalidationTest : AbstractSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.MODULE_STATE_MODIFICATION
}

abstract class AbstractModuleOutOfBlockModificationSessionInvalidationTest : AbstractSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractGlobalModuleStateModificationSessionInvalidationTest : AbstractSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceModuleStateModificationSessionInvalidationTest : AbstractSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceOutOfBlockModificationSessionInvalidationTest : AbstractSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
}
