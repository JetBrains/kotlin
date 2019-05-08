/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import java.util.*

fun Project.enableTasksIfAtLeast(productVersion: String, expectedProductBranch: Int) {
    val productBranch = productVersion.substringBefore('.').toIntOrNull()
            ?: error("Invalid product version format: $productVersion")

    if (productBranch >= expectedProductBranch)
        return // OK, nothing to disable

    // otherwise: disable build tasks
    disableBuildTasks { "$productVersion is NOT at least $expectedProductBranch" }
}

fun Project.enableTasksIfOsIsNot(vararg osNames: String) {
    osNames.forEach { osName ->
        if (osName.isBlank() || osName.trim() != osName)
            error("Invalid OS name: $osName")
    }

    if (osNames.any { it.toLowerCase(Locale.US) in hostOsName }) {
        disableBuildTasks { "\"$hostOsName\" is NOT one of ${osNames.joinToString { "\"$it\"" }}" }
    }
}

// disable anything but "clean" and tasks from "help" group
// log the appropriate message
private fun Project.disableBuildTasks(message: () -> String) {
    val tasksToDisable = tasks.filter {
        it.enabled && it.name != "clean" && it.group != "help"
    }

    if (tasksToDisable.isNotEmpty()) {
        tasksToDisable.forEach { it.enabled = false }
        logger.warn("Build tasks in $project have been disabled due to condition mismatch: ${message()}: ${tasksToDisable.joinToString { it.name }}")
    }
}
