/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.task.*
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.plugins.gradle.util.GradleConstants

internal fun ModuleBuildTask.isSupported() =
    when (this) {
        is ModuleFilesBuildTask, is ModuleResourcesBuildTask -> false
        else -> true
    }

class MPPProjectTaskRunner : ProjectTaskRunner() {
    override fun canRun(project: Project, task: ProjectTask): Boolean {
        val workspace = MPPWorkspace.getInstance(project)

        fun canBuildModule(module: Module): Boolean =
            ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module) && hasKonanModules(module.project)

        return when (task) {
            is ModuleBuildTask -> task.isSupported() && canBuildModule(task.module)
            else -> false
        }
    }

    /* This method has limited usage, so it's safe to always return false here */
    override fun canRun(task: ProjectTask): Boolean = false

    private fun hasKonanModules(project: Project): Boolean =
        CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                ModuleManager.getInstance(project).modules.any(::isKonanModule),
                ProjectRootModificationTracker.getInstance(project)
            )
        }

    private fun isKonanModule(module: Module): Boolean =
        KotlinFacet.get(module)?.configuration?.settings?.targetPlatform?.isNative() == true

}