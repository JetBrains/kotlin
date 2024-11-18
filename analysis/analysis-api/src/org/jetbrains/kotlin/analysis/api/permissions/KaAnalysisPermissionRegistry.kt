/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.permissions

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

/**
 * [KaAnalysisPermissionRegistry] stores settings required by permission functions such as [forbidAnalysis], [allowAnalysisOnEdt], and
 * [allowAnalysisFromWriteAction].
 *
 * [KaAnalysisPermissionRegistry] is an *application service* because we want users to call permission functions without having to pass a
 * project, which would be required if this class was a project service.
 */
@KaImplementationDetail
public interface KaAnalysisPermissionRegistry {
    @KaImplementationDetail
    public class KaExplicitAnalysisRestriction(public val description: String)

    public var explicitAnalysisRestriction: KaExplicitAnalysisRestriction?

    public var isAnalysisAllowedOnEdt: Boolean

    public var isAnalysisAllowedInWriteAction: Boolean

    @KaImplementationDetail
    public companion object {
        public fun getInstance(): KaAnalysisPermissionRegistry =
            ApplicationManager.getApplication().getService(KaAnalysisPermissionRegistry::class.java)
    }
}
