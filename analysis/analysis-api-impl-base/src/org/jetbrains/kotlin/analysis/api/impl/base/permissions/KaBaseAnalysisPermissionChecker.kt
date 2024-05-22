/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.permissions

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry
import org.jetbrains.kotlin.analysis.providers.KaCachedService
import org.jetbrains.kotlin.analysis.providers.permissions.KaAnalysisPermissionChecker
import org.jetbrains.kotlin.analysis.providers.permissions.KotlinAnalysisPermissionOptions

internal class KaBaseAnalysisPermissionChecker : KaAnalysisPermissionChecker {
    /**
     * Caches [KaAnalysisPermissionRegistry] to avoid repeated `getService` calls in [analyze][org.jetbrains.kotlin.analysis.api.analyze]
     * and validity assertions.
     */
    @KaCachedService
    private val permissionRegistry by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaAnalysisPermissionRegistry.getInstance()
    }

    /**
     * Caches [KotlinAnalysisPermissionOptions] to avoid repeated `getService` calls in [analyze][org.jetbrains.kotlin.analysis.api.analyze]
     * and validity assertions.
     */
    @KaCachedService
    private val permissionOptions by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinAnalysisPermissionOptions.getInstance()
    }

    override fun isAnalysisAllowed(): Boolean {
        val application = ApplicationManager.getApplication()

        if (isProhibitedEdtAnalysis(application)) return false
        if (isProhibitedWriteActionAnalysis(application)) return false
        if (permissionRegistry.explicitAnalysisRestriction != null) return false

        return true
    }

    override fun getRejectionReason(): String {
        val application = ApplicationManager.getApplication()

        if (isProhibitedEdtAnalysis(application)) {
            return "Called in the EDT thread."
        }

        if (isProhibitedWriteActionAnalysis(application)) {
            return "Called from a write action."
        }

        permissionRegistry.explicitAnalysisRestriction?.let { restriction ->
            return "Resolve is explicitly forbidden in the current action: ${restriction.description}."
        }

        error("Cannot get a rejection reason when analysis is allowed.")
    }

    private fun isProhibitedEdtAnalysis(application: Application): Boolean =
        application.isDispatchThread &&
                !permissionOptions.defaultIsAnalysisAllowedOnEdt &&
                !permissionRegistry.isAnalysisAllowedOnEdt

    private fun isProhibitedWriteActionAnalysis(application: Application): Boolean =
        application.isWriteAccessAllowed &&
                !permissionOptions.defaultIsAnalysisAllowedInWriteAction &&
                !permissionRegistry.isAnalysisAllowedInWriteAction
}
