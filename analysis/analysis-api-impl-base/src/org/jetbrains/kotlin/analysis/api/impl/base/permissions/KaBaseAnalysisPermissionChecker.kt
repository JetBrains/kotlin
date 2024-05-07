/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.permissions

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry
import org.jetbrains.kotlin.analysis.providers.KaCachedService
import org.jetbrains.kotlin.analysis.providers.permissions.KaAnalysisPermissionChecker

internal class KaBaseAnalysisPermissionChecker : KaAnalysisPermissionChecker {
    /**
     * Caches [KaAnalysisPermissionRegistry] to avoid repeated `getService` calls in [analyze][org.jetbrains.kotlin.analysis.api.analyze]
     * and validity assertions.
     */
    @KaCachedService
    private val permissionRegistry by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaAnalysisPermissionRegistry.getInstance()
    }

    override fun isAnalysisAllowed(): Boolean {
        val application = ApplicationManager.getApplication()

        if (application.isDispatchThread && !permissionRegistry.isAnalysisAllowedOnEdt) return false
        if (application.isWriteAccessAllowed && !permissionRegistry.isAnalysisAllowedInWriteAction) return false
        if (permissionRegistry.explicitAnalysisRestriction != null) return false

        return true
    }

    override fun getRejectionReason(): String {
        val application = ApplicationManager.getApplication()

        if (application.isDispatchThread && !permissionRegistry.isAnalysisAllowedOnEdt) {
            return "Called in the EDT thread."
        }

        if (application.isWriteAccessAllowed && !permissionRegistry.isAnalysisAllowedInWriteAction) {
            return "Called from a write action."
        }

        permissionRegistry.explicitAnalysisRestriction?.let { restriction ->
            return "Resolve is explicitly forbidden in the current action: ${restriction.description}."
        }

        error("Cannot get a rejection reason when analysis is allowed.")
    }
}
