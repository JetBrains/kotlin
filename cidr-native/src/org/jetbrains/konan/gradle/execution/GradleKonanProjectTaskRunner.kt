/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.isExternalSystemAwareModule
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectModelBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskNotification
import com.intellij.task.ProjectTaskRunner
import org.jetbrains.konan.KonanBundle.message
import org.jetbrains.konan.gradle.GradleKonanWorkspace
import org.jetbrains.konan.gradle.KonanModel
import org.jetbrains.konan.gradle.KonanProjectDataService
import org.jetbrains.kotlin.idea.configuration.externalProjectPath
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.platform.impl.isKotlinNative
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * TODO: Think how to merge with [KotlinMPPGradleProjectTaskRunner] in IDEA plugin.
 */
class GradleKonanProjectTaskRunner : ProjectTaskRunner() {

    override fun canRun(project: Project, task: ProjectTask): Boolean {
        val workspace = GradleKonanWorkspace.getInstance(project)
        if (!workspace.isInitialized) return false

        fun canBuildModule(module: Module): Boolean =
                isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module) && isProjectWithNativeModules(module.project)

        fun canBuildConfiguration(configuration: Any?): Boolean {
            val buildConfiguration = configuration as? GradleKonanConfiguration ?: return false
            val selectedBuildConfiguration = workspace.selectedBuildConfiguration ?: return false

            return buildConfiguration.id == selectedBuildConfiguration.id
        }

        return when (task) {
            is ModuleBuildTask -> task.isSupported() && canBuildModule(task.module)
            is GradleKonanCleanTask -> canBuildConfiguration(task.buildConfiguration)
            is ProjectModelBuildTask<*> -> canBuildConfiguration(task.buildableElement)
            else -> false
        }
    }

    /* This method has limited usage, so it's safe to always return false here */
    override fun canRun(task: ProjectTask): Boolean = false

    override fun run(project: Project, context: ProjectTaskContext, notification: ProjectTaskNotification?, tasks: Collection<ProjectTask>) {
        if (tasks.isEmpty()) return

        val gradleTasksMap = mutableMapOf<String, GradleBuildTasks>()
        val consumer = { projectPath: String, compileTask: String?, cleanupTask: String? ->
            with(gradleTasksMap.computeIfAbsent(projectPath) { GradleBuildTasks() }) {
                compileTasks.addIfNotNull(compileTask)
                cleanupTasks.addIfNotNull(cleanupTask)
            }
        }

        tasks.forEach { task ->
            when (task) {
                is ModuleBuildTask -> task.collectGradleTasks(consumer)
                is GradleKonanCleanTask -> task.collectGradleTasks(consumer)
                is ProjectModelBuildTask<*> -> task.collectGradleTasks(consumer)
            }
        }

        val toExecute = gradleTasksMap.entries.mapNotNull { (projectPath, buildTasks) ->
            val executionName = evaluateExecutionName(project, buildTasks) ?: return@mapNotNull null
            Triple(executionName, buildTasks.allTasks, projectPath)
        }

        if (toExecute.isEmpty()) return

        ApplicationManager.getApplication().executeOnPooledThread {
            toExecute.forEach { (executionName, buildTasks, projectPath) ->
                GradleKonanBuild.runBuildTasks(project, executionName, buildTasks, projectPath, true)
            }
        }
    }

    private fun isProjectWithNativeModules(project: Project): Boolean =
            CachedValuesManager.getManager(project).getCachedValue(project) {
                CachedValueProvider.Result(
                        ModuleManager.getInstance(project).modules.any(::isNativeModule),
                        ProjectRootModificationTracker.getInstance(project)
                )
            }

    private fun isNativeModule(module: Module): Boolean =
            KotlinFacet.get(module)?.configuration?.settings?.platform?.isKotlinNative == true

    private fun ModuleBuildTask.collectGradleTasks(
            consumer: (String, String?, String?) -> Unit
    ) {
        val linkedExternalProjectPath = module.externalProjectPath ?: return
        val project = module.project

        val cache = CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                    mutableMapOf<String, Pair<KonanModel, String>>().apply {
                        KonanProjectDataService.forEachKonanProject(project) { konanModel: KonanModel, moduleNode: DataNode<ModuleData>, projectPath: String ->
                            this[moduleNode.data.linkedExternalProjectPath] = konanModel to projectPath
                        }
                    }.toMap(),
                    ProjectRootModificationTracker.getInstance(project)
            )
        }

        val (konanModel, projectPath) = cache[linkedExternalProjectPath] ?: return

        val compileTask = konanModel.buildTaskPath
        val cleanupTask = if (!isIncrementalBuild) konanModel.cleanTaskPath else null

        consumer(projectPath, compileTask, cleanupTask)
    }

    private fun GradleKonanCleanTask.collectGradleTasks(
            consumer: (String, String?, String?) -> Unit
    ) {
        (buildConfiguration as? GradleKonanConfiguration)?.let { konanConfiguration ->
            val projectPath = konanConfiguration.projectPath
            val cleanupTask = konanConfiguration.artifactCleanTaskPath

            consumer(projectPath, null, cleanupTask)
        }
    }

    private fun ProjectModelBuildTask<*>.collectGradleTasks(
            consumer: (String, String?, String?) -> Unit
    ) {
        (buildableElement as? GradleKonanConfiguration)?.let { konanConfiguration ->
            val projectPath = konanConfiguration.projectPath
            val compileTask = konanConfiguration.artifactBuildTaskPath
            val cleanupTask = if (!isIncrementalBuild) konanConfiguration.artifactCleanTaskPath else null

            consumer(projectPath, compileTask, cleanupTask)
        }
    }

    private fun evaluateExecutionName(project: Project, gradleBuildTasks: GradleBuildTasks) =
            when (gradleBuildTasks.compileTasks.isEmpty()) {
                true -> when (gradleBuildTasks.cleanupTasks.isEmpty()) {
                    true -> null
                    else -> message("execution.cleanProject.name", project.name)
                }
                false -> when (gradleBuildTasks.cleanupTasks.isEmpty()) {
                    true -> message("execution.buildProject.name", project.name)
                    false -> message("execution.rebuildProject.name", project.name)
                }
            }

    private data class GradleBuildTasks(
            val cleanupTasks: MutableCollection<String> = mutableSetOf(),
            val compileTasks: MutableCollection<String> = mutableSetOf()
    ) {
        val allTasks get() = (cleanupTasks.asSequence() + compileTasks.asSequence()).toList()
    }
}
