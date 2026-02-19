/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventKind
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule

abstract class AbstractAnalysisSessionInvalidationTest : AbstractSessionInvalidationTest<KaSession>() {
    override val testOutputSubdirectoryName: String get() = "analysisSession"

    override fun getSessions(ktTestModule: KtTestModule): List<TestSession<KaSession>> {
        val sessionProvider = KaSessionProvider.getInstance(ktTestModule.ktModule.project)
        val analysisSession = sessionProvider.getAnalysisSession(ktTestModule.ktModule)
        return listOf(AnalysisTestSession(ktTestModule, analysisSession))
    }

    /**
     * The analysis session cache disregards whether libraries were invalidated during global invalidation, so some valid library analysis
     * sessions may have been evicted from the cache and should not be checked for validity.
     */
    override fun shouldSkipValidityCheck(session: TestSession<KaSession>): Boolean =
        when (modificationEventKind) {
            KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
            KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
                -> {
                val useSiteModule = session.underlyingSession.useSiteModule
                useSiteModule is KaLibraryModule || useSiteModule is KaLibrarySourceModule
            }
            else -> false
        }
}

internal class AnalysisTestSession(
    override val ktTestModule: KtTestModule,
    override val underlyingSession: KaSession,
) : TestSession<KaSession>() {
    override val isValid: Boolean
        get() = underlyingSession.isValid()

    override val description: String
        get() = buildString {
            val kaModule = ktTestModule.ktModule
            append(kaModule)

            // Analysis sessions are always "resolvable", so we're marking the analysis session for a `KaLibraryModule` as a resolvable
            // session to be consistent with the LL FIR test results.
            if (kaModule is KaLibraryModule) {
                append(" (resolvable session)")
            }
        }
}

abstract class AbstractModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_STATE_MODIFICATION
}

abstract class AbstractModuleOutOfBlockModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractGlobalModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceOutOfBlockModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractCodeFragmentContextModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION
}
