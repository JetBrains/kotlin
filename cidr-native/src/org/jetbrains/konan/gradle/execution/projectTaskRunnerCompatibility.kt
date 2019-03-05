/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.task.ModuleBuildTask
import com.intellij.task.ModuleFilesBuildTask
import com.intellij.task.impl.AbstractProjectTask
import com.jetbrains.cidr.execution.CidrBuildConfiguration
import org.jetbrains.konan.KonanBundle

internal fun ModuleBuildTask.isSupported() =
        when (this) {
            is ModuleFilesBuildTask -> false
            else -> true
        }

// Replacement for `CidrCleanTask` in bunch cidr183
internal class GradleKonanCleanTask(buildConfiguration: GradleKonanConfiguration) : AbstractProjectTask() {

    // compatibility stuff:
    val buildConfiguration: CidrBuildConfiguration = buildConfiguration

    override fun getPresentableName() = KonanBundle.message("action.clean.text")
}
