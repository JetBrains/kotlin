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
import org.jetbrains.konan.gradle.execution.GradleBuildTasksOrigin.*
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

        val buildTasksMap = GradleBuildTasksMap()
        tasks.forEach { task ->
            when (task) {
                is ModuleBuildTask -> task.collectGradleTasks(buildTasksMap)
                is GradleKonanCleanTask -> task.collectGradleTasks(buildTasksMap)
                is ProjectModelBuildTask<*> -> task.collectGradleTasks(buildTasksMap)
                else -> error("Unexpected task type: $task")
            }
        }

        if (buildTasksMap.isEmpty()) return

        val toExecute = buildTasksMap.entries.mapNotNull { (projectPath, buildTasks) ->
            val executionName = guessExecutionName(buildTasks) ?: return@mapNotNull null
            Triple(executionName, buildTasks.allTasks, projectPath)
        }

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

    private fun ModuleBuildTask.collectGradleTasks(buildTasksMap: GradleBuildTasksMap) {
        val linkedExternalProjectPath = module.externalProjectPath ?: return
        val project = module.project

        val cache = CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                    mutableMapOf<String, Pair<KonanModel, String>>().apply {
                        KonanProjectDataService.forEachKonanProject(project) { konanModel: KonanModel, moduleNode: DataNode<ModuleData>, projectPath: String ->
                            this[moduleNode.data.linkedExternalProjectPath] = konanModel to projectPath
                        }
                    } as Map<String, Pair<KonanModel, String>>,
                    ProjectRootModificationTracker.getInstance(project)
            )
        }

        val (konanModel, projectPath) = cache[linkedExternalProjectPath] ?: return

        val cleanupTask = if (!isIncrementalBuild) konanModel.cleanTaskPath else null
        val compileTask = konanModel.buildTaskPath

        buildTasksMap.consume(FromProject(project), projectPath, cleanupTask = cleanupTask, compileTask = compileTask)
    }

    private fun GradleKonanCleanTask.collectGradleTasks(buildTasksMap: GradleBuildTasksMap) {
        (buildConfiguration as? GradleKonanConfiguration)?.let { buildConfiguration ->
            val projectPath = buildConfiguration.projectPath
            val cleanupTask = buildConfiguration.artifactCleanTaskPath

            buildTasksMap.consume(FromConfiguration(buildConfiguration), projectPath, cleanupTask = cleanupTask)
        }
    }

    private fun ProjectModelBuildTask<*>.collectGradleTasks(buildTasksMap: GradleBuildTasksMap) {
        (buildableElement as? GradleKonanConfiguration)?.let { buildConfiguration ->
            val projectPath = buildConfiguration.projectPath
            val cleanupTask = if (!isIncrementalBuild) buildConfiguration.artifactCleanTaskPath else null
            val compileTask = buildConfiguration.artifactBuildTaskPath

            buildTasksMap.consume(FromConfiguration(buildConfiguration), projectPath, cleanupTask = cleanupTask, compileTask = compileTask)
        }
    }

    private fun guessExecutionName(buildTasks: GradleBuildTasks): String? {
        return when (buildTasks.compileTasks.isEmpty()) {
            true -> when (buildTasks.cleanupTasks.isEmpty()) {
                true -> null
                else -> buildTasks.origins.first().cleanExecutionName
            }
            false -> when (buildTasks.cleanupTasks.isEmpty()) {
                true -> buildTasks.origins.first().buildExecutionName
                false -> buildTasks.origins.first().rebuildExecutionName
            }
        }
    }
}

private class GradleBuildTasksMap {
    private val data = mutableMapOf<String, GradleBuildTasks>()

    val entries get() = (data as Map<String, GradleBuildTasks>).entries

    fun consume(origin: GradleBuildTasksOrigin, projectPath: String, cleanupTask: String? = null, compileTask: String? = null) {
        if (cleanupTask == null && compileTask == null) return

        with(data.computeIfAbsent(projectPath) { GradleBuildTasks() }) {
            compileTasks.addIfNotNull(compileTask)
            cleanupTasks.addIfNotNull(cleanupTask)
            origins += origin
        }
    }

    fun isEmpty() = data.values.none { it.cleanupTasks.isNotEmpty() || it.compileTasks.isNotEmpty() }
}

private sealed class GradleBuildTasksOrigin {
    abstract val buildExecutionName: String
    abstract val rebuildExecutionName: String
    abstract val cleanExecutionName: String

    class FromProject(private val project: Project): GradleBuildTasksOrigin() {
        override val buildExecutionName get() = message("execution.buildProject.name", project.name)
        override val rebuildExecutionName get() = message("execution.rebuildProject.name", project.name)
        override val cleanExecutionName get() = message("execution.cleanProject.name", project.name)
    }

    class FromConfiguration(private val buildConfiguration: GradleKonanConfiguration): GradleBuildTasksOrigin() {
        override val buildExecutionName get() = message("execution.buildConfiguration.name", buildConfiguration.name)
        override val rebuildExecutionName get() = message("execution.rebuildConfiguration.name", buildConfiguration.name)
        override val cleanExecutionName get() = message("execution.cleanConfiguration.name", buildConfiguration.name)
    }
}

private class GradleBuildTasks {
    val cleanupTasks = mutableSetOf<String>()
    val compileTasks = mutableSetOf<String>()
    val origins = mutableListOf<GradleBuildTasksOrigin>()

    val allTasks get() = (cleanupTasks.asSequence() + compileTasks.asSequence()).toList()
}
