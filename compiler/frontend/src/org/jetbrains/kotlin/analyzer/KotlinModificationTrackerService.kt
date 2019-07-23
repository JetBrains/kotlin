/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker

open class KotlinModificationTrackerService {
    open val modificationTracker: ModificationTracker = ModificationTracker.NEVER_CHANGED
    open val outOfBlockModificationTracker: ModificationTracker = ModificationTracker.NEVER_CHANGED

    companion object {
        private val NEVER_CHANGE_TRACKER_SERVICE = KotlinModificationTrackerService()

        @JvmStatic
        fun getInstance(project: Project): KotlinModificationTrackerService {
            return ServiceManager.getService(project, KotlinModificationTrackerService::class.java) ?: NEVER_CHANGE_TRACKER_SERVICE
        }
    }
}