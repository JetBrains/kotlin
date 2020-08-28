/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService

class ReadActionConfinementValidityToken(project: Project) : ValidityToken() {
    private val modificationTracker = KotlinModificationTrackerService.getInstance(project).modificationTracker
    private val onCreatedTimeStamp = modificationTracker.modificationCount

    override fun isValid(): Boolean {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) return false
        if (!application.isReadAccessAllowed) return false
        return onCreatedTimeStamp == modificationTracker.modificationCount
    }

    override fun getInvalidationReason(): String {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) return "Called in EDT thread"
        if (!application.isReadAccessAllowed) return "Called outside read action"
        if (onCreatedTimeStamp != modificationTracker.modificationCount) return "PSI has changed since creation"
        error("Getting invalidation reason for valid validity token")
    }
}