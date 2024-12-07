/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.permissions

import org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry
import org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry.KaExplicitAnalysisRestriction

internal class KaBaseAnalysisPermissionRegistry : KaAnalysisPermissionRegistry {
    private val threadLocalExplicitAnalysisRestriction: ThreadLocal<KaExplicitAnalysisRestriction?> =
        ThreadLocal.withInitial { null }

    private val threadLocalAllowOnEdt: ThreadLocal<Boolean> =
        ThreadLocal.withInitial { false }

    private val threadLocalAllowInWriteAction: ThreadLocal<Boolean> =
        ThreadLocal.withInitial { false }

    override var explicitAnalysisRestriction: KaExplicitAnalysisRestriction?
        get() = threadLocalExplicitAnalysisRestriction.get()
        set(value) = threadLocalExplicitAnalysisRestriction.set(value)

    override var isAnalysisAllowedOnEdt: Boolean
        get() = threadLocalAllowOnEdt.get()
        set(value) = threadLocalAllowOnEdt.set(value)

    override var isAnalysisAllowedInWriteAction: Boolean
        get() = threadLocalAllowInWriteAction.get()
        set(value) = threadLocalAllowInWriteAction.set(value)
}
