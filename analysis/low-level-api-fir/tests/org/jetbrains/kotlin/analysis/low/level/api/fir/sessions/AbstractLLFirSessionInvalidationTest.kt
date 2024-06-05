/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventKind
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator

/**
 * An [AbstractSessionInvalidationTest] for [LLFirSession].
 *
 * See also `AbstractSessionInvalidationTest` on the IDE side, which is still required to check session invalidation in module structures
 * with cyclic dependencies.
 */
abstract class AbstractLLFirSessionInvalidationTest : AbstractSessionInvalidationTest<LLFirSession>() {
    override val resultFileSuffix: String? get() = null

    override fun getSession(ktModule: KtModule): LLFirSession =
        LLFirSessionCache.getInstance(ktModule.project).getSession(ktModule, preferBinary = true)

    override fun getSessionKtModule(session: LLFirSession): KtModule = session.ktModule
    override fun isSessionValid(session: LLFirSession): Boolean = session.isValid

    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
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
