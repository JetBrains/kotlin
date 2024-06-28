/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.permissions

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry.KaExplicitAnalysisRestriction

/**
 * [KaAnalysisPermissionRegistry] stores settings required by permission functions such as [forbidAnalysis], [allowAnalysisOnEdt], and
 * [allowAnalysisFromWriteAction].
 *
 * [KaAnalysisPermissionRegistry] is an *application service* because we want users to call permission functions without having to pass a
 * project, which would be required if this class was a project service.
 */
@KaAnalysisApiInternals
public interface KaAnalysisPermissionRegistry {
    public class KaExplicitAnalysisRestriction(public val description: String)

    public var explicitAnalysisRestriction: KaExplicitAnalysisRestriction?

    public var isAnalysisAllowedOnEdt: Boolean

    public var isAnalysisAllowedInWriteAction: Boolean

    public companion object {
        private val permissionRegistry = KaAnalysisPermissionRegistryImpl()

        /**
         * This [getInstance] has the exact same signature as a `getInstance` for an application service, to allow implementing
         * [KaAnalysisPermissionRegistry] as an application service in the future when KT-68386 has been fixed.
         */
        public fun getInstance(): KaAnalysisPermissionRegistry = permissionRegistry
    }
}

/**
 * This implementation is a workaround for KT-68386, as we currently cannot register it as an application service in Standalone mode.
 */
@OptIn(KaAnalysisApiInternals::class)
private class KaAnalysisPermissionRegistryImpl : KaAnalysisPermissionRegistry {
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
