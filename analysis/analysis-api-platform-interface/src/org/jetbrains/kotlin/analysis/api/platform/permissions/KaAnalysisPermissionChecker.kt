/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.permissions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KaEngineService

/**
 * [KaAnalysisPermissionChecker] is an *engine service* which allows checking whether analysis is currently allowed.
 *
 * In general, analysis can be prohibited in the following cases:
 *
 * - If analysis is invoked from the EDT, it is prohibited unless explicitly allowed via
 *   [allowAnalysisOnEdt][org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt] or [KotlinAnalysisPermissionOptions].
 * - If analysis is invoked from a write action, it is prohibited unless explicitly allowed via
 *   [allowAnalysisFromWriteAction][org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisFromWriteAction] or
 *   [KotlinAnalysisPermissionOptions].
 * - Analysis can also be explicitly forbidden via [forbidAnalysis][org.jetbrains.kotlin.analysis.api.permissions.forbidAnalysis], which in
 *   contrast to the above points cannot be controlled with [KotlinAnalysisPermissionOptions].
 */
public interface KaAnalysisPermissionChecker : KaEngineService {
    public fun isAnalysisAllowed(): Boolean

    public fun getRejectionReason(): String

    public companion object {
        public fun getInstance(project: Project): KaAnalysisPermissionChecker = project.service()
    }
}
