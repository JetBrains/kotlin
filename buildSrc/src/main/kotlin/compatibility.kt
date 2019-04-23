/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project

fun Project.enableTasksIfAtLeast(productVersion: String, expectedProductBranch: Int) {
    val productBranch = productVersion.substringBefore('.').toIntOrNull()
            ?: error("Invalid product version format: $productVersion")

    if (productBranch >= expectedProductBranch)
        return // OK, nothing to disable

    // otherwise: disable anything but "clean" and tasks from "help" group
    tasks.filter { it.name != "clean" && it.group != "help" }.forEach { task ->
        task.enabled = false
        logger.kotlinInfo { "Task ${task.path} has been disabled due to condition mismatch: $productVersion is NOT at least $expectedProductBranch" }
    }
}
