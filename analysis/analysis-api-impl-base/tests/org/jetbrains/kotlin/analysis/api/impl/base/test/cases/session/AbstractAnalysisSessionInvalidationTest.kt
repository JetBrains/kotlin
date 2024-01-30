/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.test.framework.directives.ModificationEventKind

abstract class AbstractAnalysisSessionInvalidationTest : AbstractSessionInvalidationTest<KtAnalysisSession>() {
    override val resultFileSuffix: String get() = "analysis_session"

    override fun getSession(ktModule: KtModule) =
        KtAnalysisSessionProvider.getInstance(ktModule.project).getAnalysisSessionByUseSiteKtModule(ktModule)

    override fun getSessionKtModule(session: KtAnalysisSession): KtModule = session.useSiteModule
    override fun isSessionValid(session: KtAnalysisSession): Boolean = session.isValid()

    /**
     * The analysis session cache disregards whether libraries were invalidated during global invalidation, so some valid library analysis
     * sessions may have been evicted from the cache and should not be checked for validity.
     */
    override fun shouldSkipValidityCheck(session: KtAnalysisSession): Boolean =
        when (modificationEventKind) {
            ModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
            ModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
            -> {
                session.useSiteModule is KtBinaryModule || session.useSiteModule is KtLibrarySourceModule
            }
            else -> false
        }
}

abstract class AbstractModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.MODULE_STATE_MODIFICATION
}

abstract class AbstractModuleOutOfBlockModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractGlobalModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceOutOfBlockModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractCodeFragmentContextModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: ModificationEventKind
        get() = ModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION
}
