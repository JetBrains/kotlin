/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.util.containers.ContainerUtil.set
import com.intellij.util.io.isDirectory
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.plugin.model.KonanModel
import org.jetbrains.kotlin.gradle.plugin.model.KonanModelArtifact
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Files.isDirectory
import java.nio.file.Files.walk
import java.nio.file.Path
import java.util.*

/**
 * [KonanProjectResolver] creates IDE project model in terms of External System API
 */
class KonanProjectResolver : AbstractProjectResolverExtension() {

    // to ask gradle for the model
    override fun getExtraProjectModelClasses(): Set<Class<*>> {
        return set<Class<*>>(KonanModel::class.java)
    }

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        resolverCtx.getExtraProject(gradleModule, KonanModel::class.java)?.let {
            // store a local process copy of the object to get rid of proxy types for further serialization
            ideModule.createChild(KONAN_MODEL_KEY, MyKonanModel(it))
        }

        nextResolver.populateModuleExtraModels(gradleModule, ideModule)
    }

    override fun populateModuleContentRoots(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        resolverCtx.getExtraProject(gradleModule, KonanModel::class.java)?.let {
            val added = mutableSetOf<String>()
            for (artifact in it.artifacts) {
                for (srcDir in artifact.srcDirs) {
                    val rootPath = srcDir.absolutePath
                    if (!added.add(rootPath)) continue

                    val ideContentRoot = ContentRootData(GradleConstants.SYSTEM_ID, rootPath)
                    ideContentRoot.storePath(ExternalSystemSourceType.SOURCE, rootPath)
                    ideModule.createChild(ProjectKeys.CONTENT_ROOT, ideContentRoot)
                }
            }
        }

        nextResolver.populateModuleContentRoots(gradleModule, ideModule)
    }

    // based on KonanCMakeProjectComponent.reloadLibraries but with the multi-module projects support
    override fun populateModuleDependencies(
        gradleModule: IdeaModule,
        ideModule: DataNode<ModuleData>,
        ideProject: DataNode<ProjectData>
    ) {
        val konanModelEx = resolverCtx.getExtraProject(gradleModule, KonanModel::class.java)
        if (konanModelEx != null) {
            val libraryPaths = LinkedHashSet<Path>()
            var konanHome: Path? = null
            var targetPlatform: String? = null
            for (konanArtifact in konanModelEx.artifacts) {
                if (konanHome == null) {
                    konanHome = konanModelEx.konanHome.toPath()
                    targetPlatform = konanArtifact.targetPlatform
                }
                konanArtifact.libraries.forEach { libraryPaths.add(it.toPath()) }
            }

            if (konanHome != null) {
                // add konanStdlib copied from KonanPaths.konanStdlib
                libraryPaths.add(konanHome.resolve("klib/common/stdlib"))

                // add konanPlatformLibraries, copied from KonanPaths.konanPlatformLibraries
                if (targetPlatform != null) {
                    try {
                        val resolvedTargetName = HostManager.resolveAlias(targetPlatform)
                        val klibPath = konanHome.resolve("klib/platform/${resolvedTargetName}")
                        walk(klibPath, 1)
                            .filter { it.isDirectory() && it.fileName.toString() != "stdlib" && it != klibPath }
                            .forEach { libraryPaths.add(it) }
                    } catch (e: Exception) {
                        LOG.warn("Unable to collect konan platform libraries paths for '$targetPlatform'", e)
                    }
                }
            }

            val moduleData = ideModule.data
            for (path in libraryPaths) {
                val library = LibraryData(moduleData.owner, path.fileName.toString())
                library.addPath(LibraryPathType.BINARY, if (isDirectory(path)) path.toAbsolutePath().toString() else path.toString())

                val data = LibraryDependencyData(moduleData, library, LibraryLevel.MODULE)
                ideModule.createChild(ProjectKeys.LIBRARY_DEPENDENCY, data)
            }
        }

        nextResolver.populateModuleDependencies(gradleModule, ideModule, ideProject)
    }

    private class MyKonanModel(konanModel: KonanModel) : KonanModel {
        override val artifacts: List<KonanModelArtifact> = konanModel.artifacts.map { MyKonanArtifactEx(it) }
        override val konanHome: File = konanModel.konanHome
        override val konanVersion: KonanVersion = MyKonanVersionEx(konanModel.konanVersion)
        override val apiVersion: String? = konanModel.apiVersion
        override val languageVersion: String? = konanModel.languageVersion

        private class MyKonanVersionEx(version: KonanVersion) : KonanVersion {
            override val build: Int = version.build
            override val maintenance: Int = version.maintenance
            override val major: Int = version.major
            override val meta: MetaVersion = version.meta
            override val minor: Int = version.minor

            override fun toString(showMeta: Boolean, showBuild: Boolean): String {
                val sb = StringBuilder("$major.$minor.$maintenance")
                if (showMeta) sb.append('-').append(meta)
                if (showBuild) sb.append('-').append(build)
                return sb.toString()
            }
        }

        private class MyKonanArtifactEx(artifact: KonanModelArtifact) : KonanModelArtifact {
            override val searchPaths: List<File> = artifact.searchPaths
            override val name: String = artifact.name
            override val type: CompilerOutputKind = artifact.type
            override val targetPlatform: String = artifact.targetPlatform
            override val file: File = artifact.file
            override val buildTaskName: String = artifact.buildTaskName

            override val srcDirs: List<File> = artifact.srcDirs.toList()
            override val srcFiles: List<File> = artifact.srcFiles.toList()
            override val libraries: List<File> = artifact.libraries.toList()
        }
    }

    companion object {
        val KONAN_MODEL_KEY = Key.create(KonanModel::class.java, ProjectKeys.MODULE.processingWeight + 1)
        private val LOG = Logger.getInstance(KonanProjectResolver::class.java)
    }
}
