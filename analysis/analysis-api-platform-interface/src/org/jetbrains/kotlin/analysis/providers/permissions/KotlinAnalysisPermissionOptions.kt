/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.permissions

import com.intellij.openapi.application.ApplicationManager

/**
 * [KotlinAnalysisPermissionOptions] gives an Analysis API platform the choice whether to allow analysis on the EDT and in write actions by
 * default.
 *
 * @see KotlinDefaultAnalysisPermissionOptions
 */
public interface KotlinAnalysisPermissionOptions {
    /**
     * The default setting for [org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry.isAnalysisAllowedOnEdt], when not
     * overridden by [org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt].
     *
     * TODO (KT-68186): Due to KT-68386, the implementation currently doesn't apply the default to
     * [KaAnalysisPermissionRegistry][org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry], but it is taken into
     * account by [KaAnalysisPermissionChecker].
     */
    public val defaultIsAnalysisAllowedOnEdt: Boolean

    /**
     * The default setting for [org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry.isAnalysisAllowedInWriteAction],
     * when not overridden by [org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisFromWriteAction].
     *
     * TODO (KT-68186): Due to KT-68386, the implementation currently doesn't apply the default to
     * [KaAnalysisPermissionRegistry][org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry], but it is taken into
     * account by [KaAnalysisPermissionChecker].
     */
    public val defaultIsAnalysisAllowedInWriteAction: Boolean

    public companion object {
        /**
         * Due to KT-68386, the service registration is currently optional, with defaults for Standalone as it cannot register its own
         * permission options.
         *
         * TODO (KT-68386): Remove this once KT-68386 is fixed.
         */
        private val standaloneDefaults = object : KotlinAnalysisPermissionOptions {
            override val defaultIsAnalysisAllowedOnEdt: Boolean get() = true
            override val defaultIsAnalysisAllowedInWriteAction: Boolean get() = true
        }

        public fun getInstance(): KotlinAnalysisPermissionOptions =
            ApplicationManager.getApplication().getService(KotlinAnalysisPermissionOptions::class.java) ?: standaloneDefaults
    }
}
