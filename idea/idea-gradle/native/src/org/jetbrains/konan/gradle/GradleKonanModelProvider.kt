/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.find
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.findAll
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.BooleanFunction
import com.sun.media.jfxmediaimpl.platform.PlatformManager
import org.jetbrains.konan.settings.KonanArtifact
import org.jetbrains.konan.settings.KonanModelProvider
import org.jetbrains.kotlin.gradle.plugin.model.KonanModel
import org.jetbrains.kotlin.gradle.plugin.model.KonanModelArtifact
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.target.customerDistribution
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Path

class GradleKonanModelProvider : KonanModelProvider {
    override fun reloadLibraries(project: Project, libraryPaths: Collection<Path>): Boolean =
        GradleSettings.getInstance(project).linkedProjectsSettings.isNotEmpty()

    override fun getKonanHome(project: Project): Path? {
        val projectNode = ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
            .mapNotNull { it.externalProjectStructure }
            .firstOrNull() ?: return null
        projectNode.getUserData(KONAN_HOME)?.let { return it }

        var konanHomePath: Path? = null
        find(projectNode, ProjectKeys.MODULE, BooleanFunction { moduleNode ->
            find(moduleNode, KonanProjectResolver.KONAN_MODEL_KEY, BooleanFunction { konanModelNode ->
                konanHomePath = konanModelNode.data.konanHome.toPath()
                projectNode.putUserData(KONAN_HOME, konanHomePath)
                konanHomePath != null
            }) != null
        })
        return konanHomePath
    }

    override fun getArtifacts(project: Project): Collection<KonanArtifact> {
        val artifacts = mutableListOf<KonanArtifact>()
        ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
            .mapNotNull { it.externalProjectStructure }
            .forEach { projectStructure ->
                findAll(projectStructure, ProjectKeys.MODULE)
                    .map { Pair(it.data.externalName, find<KonanModel>(it, KonanProjectResolver.KONAN_MODEL_KEY)) }
                    .filter { it.second != null }
                    .forEach { (moduleName, konanProjectNode) ->
                        konanProjectNode!!.data.artifacts.forEach { konanArtifact ->
                            val sources = konanArtifact.srcFiles.map { it.toPath() }

                            artifacts.add(
                                KonanArtifact(
                                    konanArtifact.name,
                                    moduleName,
                                    konanArtifact.type,
                                    konanTarget(konanProjectNode.data.konanHome, konanArtifact),
                                    mutableListOf(), sources, konanArtifact.file.toPath()
                                )
                            )
                        }
                    }
            }
        return artifacts
    }

    private fun konanTarget(konanHome: File, konanArtifactEx: KonanModelArtifact) =
        PlatformManager(customerDistribution(konanHome.absolutePath)).targetValues.find { it.name == konanArtifactEx.targetPlatform }

    companion object {
        val KONAN_HOME = Key.create<Path>("KONAN_HOME")
    }
}
