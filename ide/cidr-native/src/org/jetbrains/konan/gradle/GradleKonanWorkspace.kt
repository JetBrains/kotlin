/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.RunManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.progress.BackgroundTaskQueue
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCWorkspaceImpl
import org.jetbrains.konan.gradle.CachedBuildableElements.KonanBuildableElements
import org.jetbrains.konan.gradle.CachedBuildableElements.NoKonanBuildableElements
import org.jetbrains.konan.gradle.KonanProjectDataService.Companion.forEachKonanProject
import org.jetbrains.konan.gradle.execution.GradleKonanAppRunConfiguration
import org.jetbrains.konan.gradle.execution.GradleKonanBuildTarget
import org.jetbrains.konan.gradle.execution.GradleKonanConfiguration
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.plugins.gradle.settings.GradleSettings

class GradleKonanWorkspace(val project: Project) : ProjectComponent {

    companion object {
        private const val LOADING_GRADLE_KONAN_PROJECT = "Loading Gradle Kotlin/Native Project..."

        @JvmStatic
        fun getInstance(project: Project): GradleKonanWorkspace? {
            return if (project.hasComponent(GradleKonanWorkspace::class.java)) {
                project.getComponent(GradleKonanWorkspace::class.java)
            } else {
                null
            }
        }
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

    data class KonanModelDataKey(val moduleId: String, val rootProjectPath: String)
    data class KonanModelDataValue(val moduleData: ModuleData, val konanModel: KonanModel)

    val konanModelData = mutableMapOf<KonanModelDataKey, KonanModelDataValue>()
    val artifactNamesUsedInModules = mutableMapOf<String, MutableSet<String>>()

    // collect KonanModel objects
    // also collect the mapping between artifact names and modules where artifact with such names are used
    forEachKonanProject(project) { konanModel, moduleNode, rootProjectPath ->
        val moduleData = moduleNode.data
        val moduleId = moduleData.id

        konanModelData[KonanModelDataKey(moduleId, rootProjectPath)] = KonanModelDataValue(moduleData, konanModel)

        konanModel.artifacts.forEach { artifact ->
            artifactNamesUsedInModules.computeIfAbsent(artifact.name) { mutableSetOf() } += moduleId
        }
    }

    // if there are at least two modules which have artifacts with the same name -> need to use disambiguation suffix
    val useDisambiguationSuffix = artifactNamesUsedInModules.values.any { it.size > 1 }

    data class ConfigurationKey(val moduleId: String, val artifactName: String)
    data class ConfigurationValue(
            val moduleName: String,
            val disambiguationSuffix: String,
            val configurations: MutableList<GradleKonanConfiguration> = mutableListOf()
    )

    val configurationsMap = mutableMapOf<ConfigurationKey, ConfigurationValue>()
    konanModelData.forEach { entry ->
        val (moduleId, rootProjectPath) = entry.key
        val (moduleData, konanModel) = entry.value

        // if there are artifacts with same name, then append module name for disambiguation
        val disambiguationSuffix = if (useDisambiguationSuffix) " ($moduleId)" else ""

        konanModel.artifacts.forEach { artifact ->
            val configurationName = artifact.name + disambiguationSuffix
            val configurationId = getConfigurationId(moduleId, artifact)
            val profileName = if (artifact.buildTaskPath.contains("Debug", ignoreCase = true)) "Debug" else "Release"

            val configuration = GradleKonanConfiguration(
                    configurationId,
                    configurationName,
                    profileName,
                    artifact.file,
                    artifact.type,
                    artifact.buildTaskPath,
                    konanModel.cleanTaskPath,
                    rootProjectPath,
                    artifact.isTests
            )

            configurationsMap.computeIfAbsent(ConfigurationKey(moduleId, artifact.name)) {
                ConfigurationValue(moduleData.externalName, disambiguationSuffix)
            }.configurations += configuration
        }
    }

    val buildTargets = mutableListOf<GradleKonanBuildTarget>()
    configurationsMap.forEach { entry ->
        val (moduleId, artifactName) = entry.key
        val (moduleName, disambiguationSuffix, configurations) = entry.value

        val baseBuildTarget = configurations.filter { !it.isTests }.ifNotEmpty {
            this.createBuildTarget(moduleId, artifactName, disambiguationSuffix, moduleName)
        }

        val testBuildTarget = configurations.firstOrNull { it.isTests }?.let {
            listOf(it).createBuildTarget(moduleId, artifactName + "Tests", disambiguationSuffix, moduleName)
        }

        if (testBuildTarget != null && baseBuildTarget != null) {
            testBuildTarget.baseBuildTarget = baseBuildTarget
        }

        buildTargets.addIfNotNull(baseBuildTarget)
        buildTargets.addIfNotNull(testBuildTarget)
    }

    return if (buildTargets.isNotEmpty()) KonanBuildableElements(buildTargets) else NoKonanBuildableElements
}

private fun getConfigurationId(moduleId: String, artifact: KonanModelArtifact) =
    getBuildTargetId(moduleId, artifact.name) + ":" + artifact.buildTaskPath

private fun getBuildTargetId(moduleId: String, targetName: String) = "$moduleId:$targetName"

private fun List<GradleKonanConfiguration>.createBuildTarget(
    moduleId: String,
    targetName: String,
    disambiguationSuffix: String,
    moduleName: String
) = GradleKonanBuildTarget(getBuildTargetId(moduleId, targetName), targetName + disambiguationSuffix, moduleName, this)

private val Project.mayBeKotlinNativeProject
    get() = GradleSettings.getInstance(this).linkedProjectsSettings.isNotEmpty()
