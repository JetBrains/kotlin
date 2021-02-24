/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

object MigrationToolFUSCollector {

    fun logNotification(oldVersions: Map<String, String>) {
        KotlinFUSLogger.log(FUSEventGroups.MigrationTool, Events.Notification.toString(), oldVersions)
    }

    fun logRun() {
        KotlinFUSLogger.log(FUSEventGroups.MigrationTool, Events.Run.toString())
    }

    enum class Events {
        Notification, Run
    }
}