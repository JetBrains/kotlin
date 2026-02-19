/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractSessionInvalidationTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.TestSession
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventKind
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator

/**
 * An [AbstractSessionInvalidationTest] for [LLFirSession].
 *
 * See also `AbstractSessionInvalidationTest` on the IDE side, which is still required to check session invalidation in module structures
 * with cyclic dependencies.
 */
abstract class AbstractLLFirSessionInvalidationTest : AbstractSessionInvalidationTest<LLFirSession>() {
    override val testOutputSubdirectoryName: String get() = "firSession"

    override fun getSessions(ktTestModule: KtTestModule): List<TestSession<LLFirSession>> {
        val kaModule = ktTestModule.ktModule
        val sessionCache = LLFirSessionCache.getInstance(kaModule.project)

        val sessions = buildList {
            add(sessionCache.getSession(kaModule, preferBinary = false))

            // `KaLibraryModule` can describe both a resolvable library session and a binary library session. So we have to make sure that
            // we also add the binary session.
            if (kaModule is KaLibraryModule) {
                add(sessionCache.getSession(kaModule, preferBinary = true))
            }
        }
        return sessions.map { LLTestSession(ktTestModule, it) }
    }

    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

internal class LLTestSession(
    override val ktTestModule: KtTestModule,
    override val underlyingSession: LLFirSession,
) : TestSession<LLFirSession>() {
    override val isValid: Boolean
        get() = underlyingSession.isValid

    override val description: String
        get() = buildString {
            val kaModule = ktTestModule.ktModule
            append(kaModule)
            if (kaModule is KaLibraryModule && underlyingSession is LLFirResolvableModuleSession) {
                append(" (resolvable session)")
            }
        }
}

abstract class AbstractModuleStateModificationLLFirSessionInvalidationTest : AbstractLLFirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_STATE_MODIFICATION
}

abstract class AbstractModuleOutOfBlockModificationLLFirSessionInvalidationTest : AbstractLLFirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractGlobalModuleStateModificationLLFirSessionInvalidationTest : AbstractLLFirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceModuleStateModificationLLFirSessionInvalidationTest : AbstractLLFirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceOutOfBlockModificationLLFirSessionInvalidationTest : AbstractLLFirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractCodeFragmentContextModificationLLFirSessionInvalidationTest : AbstractLLFirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION
}
