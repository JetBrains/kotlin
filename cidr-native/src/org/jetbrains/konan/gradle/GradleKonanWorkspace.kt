/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.RunManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.progress.BackgroundTaskQueue
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCWorkspaceImpl
import org.jetbrains.konan.gradle.CachedBuildableElements.KonanBuildableElements
import org.jetbrains.konan.gradle.CachedBuildableElements.NoKonanBuildableElements
import org.jetbrains.konan.gradle.KonanProjectDataService.Companion.forEachKonanProject
import org.jetbrains.konan.gradle.execution.GradleKonanAppRunConfiguration
import org.jetbrains.konan.gradle.execution.GradleKonanBuildTarget
import org.jetbrains.konan.gradle.execution.GradleKonanConfiguration
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.plugins.gradle.settings.GradleSettings

class GradleKonanWorkspace(val project: Project) : ProjectComponent {

    companion object {
        private const val LOADING_GRADLE_KONAN_PROJECT = "Loading Gradle Kotlin/Native Project..."

        @JvmStatic
        fun getInstance(project: Project): GradleKonanWorkspace = project.getComponent(GradleKonanWorkspace::class.java)
    }

    private val reloadsQueue = BackgroundTaskQueue(project, LOADING_GRADLE_KONAN_PROJECT)

    private val cachedBuildableElements = AtomicClearableLazyValue.create {
        if (project.mayBeKotlinNativeProject) loadBuildableElements(project) else NoKonanBuildableElements
    }

    val buildTargets: List<GradleKonanBuildTarget>
        get() = (cachedBuildableElements.value as? KonanBuildableElements)?.buildTargets ?: emptyList()

    val isInitialized: Boolean
        get() = cachedBuildableElements.value is KonanBuildableElements

    val selectedRunConfiguration: GradleKonanAppRunConfiguration?
        get() = RunManager.getInstance(project).selectedConfiguration?.configuration as? GradleKonanAppRunConfiguration

    val selectedBuildConfiguration: GradleKonanConfiguration?
        get() = selectedRunConfiguration
                ?.getBuildAndRunConfigurations(ExecutionTargetManager.getActiveTarget(project))
                ?.buildConfiguration

    override fun projectOpened() {
        // force reloading of the build targets when external data cache is ready
        ExternalProjectsManager.getInstance(project).runWhenInitialized { update() }
    }

    fun getResolveConfigurationFor(configuration: GradleKonanConfiguration?): OCResolveConfiguration? = configuration?.let {
        OCWorkspaceImpl.getInstanceImpl(project).getConfigurationById(configuration.id)
    }

    fun update() {
        // Just reset targets if no Gradle projects are linked with this IDE project.
        if (!project.mayBeKotlinNativeProject) {
            if (cachedBuildableElements.value is KonanBuildableElements) {
                cachedBuildableElements.drop()
            }

            return
        }

        reloadsQueue.run(object : Task.Backgroundable(project, LOADING_GRADLE_KONAN_PROJECT) {
            override fun run(indicator: ProgressIndicator) {
                cachedBuildableElements.drop()
                cachedBuildableElements.value
                ApplicationManager.getApplication().invokeLater(
                    Runnable { ExecutionTargetManager.update(project) },
                    project.disposed
                )
            }
        })
    }
}

private sealed class CachedBuildableElements {
    object NoKonanBuildableElements : CachedBuildableElements()
    class KonanBuildableElements(val buildTargets: List<GradleKonanBuildTarget>) : CachedBuildableElements()
}

private fun loadBuildableElements(project: Project): CachedBuildableElements {

    val buildTargets = mutableListOf<GradleKonanBuildTarget>()

    forEachKonanProject(project) { konanModel, moduleNode, rootProjectPath ->
        val moduleData = moduleNode.data

        val configurationsMap = MultiMap.createSmart<Triple<String, String, String>, GradleKonanConfiguration>()
        for (konanArtifact in konanModel.artifacts) {
            val artifactBuildTaskPath = konanArtifact.buildTaskPath
            val id = getConfigurationId(moduleData.id, konanArtifact)
            // TODO: We should do something about debug/release for gradle
            val configuration = GradleKonanConfiguration(
                id,
                konanArtifact.name,
                if (artifactBuildTaskPath.contains("ebug")) "Debug" else "Release",
                konanArtifact.file,
                konanArtifact.type,
                artifactBuildTaskPath,
                konanModel.cleanTaskPath,
                rootProjectPath,
                konanArtifact.isTests
            )
            configurationsMap.putValue(Triple(moduleData.id, moduleData.externalName, konanArtifact.name), configuration)
        }

        configurationsMap.entrySet().forEach { entry ->

            val (moduleId, moduleName, originalTargetName) = entry.key
            val value: Any = entry.value

            @Suppress("UNCHECKED_CAST")
            val configurations: List<GradleKonanConfiguration> =
                (value as? List<GradleKonanConfiguration>) ?: listOf(value as GradleKonanConfiguration)

            configurations.filter { !it.isTests }.ifNotEmpty {
                buildTargets += this.createBuildTarget(moduleId, originalTargetName, moduleName)
            }

            configurations.firstOrNull { it.isTests }?.apply {
                buildTargets += listOf(this).createBuildTarget(moduleId, originalTargetName + "Tests", moduleName)
            }
        }
    }

    return if (buildTargets.isNotEmpty()) KonanBuildableElements(buildTargets) else NoKonanBuildableElements
}

private fun getConfigurationId(moduleId: String, konanArtifact: KonanModelArtifact) =
    getBuildTargetId(moduleId, konanArtifact.name) + ":" + konanArtifact.buildTaskPath

private fun getBuildTargetId(moduleId: String, targetName: String) = "$moduleId:$targetName"

private fun List<GradleKonanConfiguration>.createBuildTarget(
    moduleId: String,
    targetName: String,
    moduleName: String
) = GradleKonanBuildTarget(getBuildTargetId(moduleId, targetName), targetName, moduleName, this)

private val Project.mayBeKotlinNativeProject
    get() = GradleSettings.getInstance(this).linkedProjectsSettings.isNotEmpty()
