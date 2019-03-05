/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.task.ModuleBuildTask
import com.intellij.task.ModuleFilesBuildTask
import com.intellij.task.ModuleResourcesBuildTask
import com.jetbrains.cidr.execution.build.tasks.CidrCleanTask

internal fun ModuleBuildTask.isSupported() =
        when (this) {
            is ModuleFilesBuildTask, is ModuleResourcesBuildTask -> false
            else -> true
        }

internal typealias GradleKonanCleanTask = CidrCleanTask
