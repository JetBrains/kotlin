/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.test.framework.directives.ModificationEventKind

abstract class AbstractAnalysisSessionInvalidationTest : AbstractSessionInvalidationTest<KtAnalysisSession>() {
    override val resultFileSuffix: String get() = "analysis_session"

    override fun getSession(ktModule: KtModule) =
        KtAnalysisSessionProvider.getInstance(ktModule.project).getAnalysisSessionByUseSiteKtModule(ktModule)

    override fun getSessionKtModule(session: KtAnalysisSession): KtModule = session.useSiteModule
    override fun isSessionValid(session: KtAnalysisSession): Boolean = session.isValid()
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
