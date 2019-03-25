/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestTasksProvider
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.util.GradleConstants

class KotlinMPPGradleTestTasksProvider : GradleTestTasksProvider {
    private companion object {
        const val TASK_NAME_SUFFIX = "Test"
        const val CLEAN_NAME_PREFIX = "clean"

        val ALLOWED_TARGETS = listOf("jvm")
    }

    override fun getTasks(module: Module): List<String> {
        if (!isTestCommonModule(module)) {
            return emptyList()
        }

        val projectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return emptyList()
        val externalProjectInfo = ExternalSystemUtil.getExternalProjectInfo(module.project, GradleConstants.SYSTEM_ID, projectPath)
            ?: return emptyList()

        val moduleData = GradleProjectResolverUtil.findModule(externalProjectInfo.externalProjectStructure, projectPath)
            ?: return emptyList()

        val gradlePath = GradleProjectResolverUtil.getGradlePath(module) ?: return emptyList()
        val taskNamePrefix = if (gradlePath.endsWith(':')) gradlePath else "$gradlePath:"

        val kotlinTaskNameCandidates = ExternalSystemApiUtil.findAll(moduleData, KotlinTargetData.KEY)
            .filter { it.data.externalName in ALLOWED_TARGETS }
            .mapTo(mutableSetOf()) { it.data.externalName + TASK_NAME_SUFFIX }

        return ExternalSystemApiUtil.findAll(moduleData, ProjectKeys.TASK)
            .filter { it.data.name in kotlinTaskNameCandidates }
            .flatMap { getTaskNames(it.data, taskNamePrefix) }
    }

    private fun isTestCommonModule(module: Module): Boolean {
        val settings = KotlinFacetSettingsProvider.getInstance(module.project).getInitializedSettings(module)
        return settings.platform.isCommon() && settings.isTestModule
    }

    private fun getTaskNames(task: TaskData, namePrefix: String): List<String> {
        val name = task.name
        return listOf(namePrefix + CLEAN_NAME_PREFIX + name.capitalize(), namePrefix + name)
    }
}