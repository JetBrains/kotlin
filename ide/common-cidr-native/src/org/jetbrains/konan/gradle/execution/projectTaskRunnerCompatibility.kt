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
