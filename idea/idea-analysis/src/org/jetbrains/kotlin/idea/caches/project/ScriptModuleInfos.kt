/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.core.script.dependencies.ScriptAdditionalIdeaDependenciesProvider
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

data class ScriptModuleInfo(
    val project: Project,
    val scriptFile: VirtualFile,
    val scriptDefinition: ScriptDefinition
) : IdeaModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.OTHER

    override val name: Name = Name.special("<script ${scriptFile.name} ${scriptDefinition.name}>")

    override fun contentScope() = GlobalSearchScope.fileScope(project, scriptFile)

    override fun dependencies(): List<IdeaModuleInfo> {
        return arrayListOf<IdeaModuleInfo>(this).apply {
            val scriptDependentModules = ScriptAdditionalIdeaDependenciesProvider.getRelatedModules(scriptFile, project)
            scriptDependentModules.forEach {
                addAll(it.correspondingModuleInfos())
            }

            val scriptDependentLibraries = ScriptAdditionalIdeaDependenciesProvider.getRelatedLibraries(scriptFile, project)
            scriptDependentLibraries.forEach {
                addAll(createLibraryInfo(project, it))
            }

            val dependenciesInfo = ScriptDependenciesInfo.ForFile(project, scriptFile, scriptDefinition)
            add(dependenciesInfo)

            dependenciesInfo.sdk?.let { add(SdkInfo(project, it)) }
        }
    }

    override val platform: TargetPlatform
        get() = JvmPlatforms.unspecifiedJvmPlatform // TODO(dsavvinov): choose proper target version

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = JvmPlatformAnalyzerServices
}

sealed class ScriptDependenciesInfo(val project: Project) : IdeaModuleInfo, BinaryModuleInfo {
    abstract val sdk: Sdk?

    override val name = Name.special("<Script dependencies>")

    override fun dependencies(): List<IdeaModuleInfo> = listOfNotNull(this, sdk?.let { SdkInfo(project, it) })

    // NOTE: intentionally not taking corresponding script info into account
    // otherwise there is no way to implement getModuleInfo
    override fun hashCode() = project.hashCode()

    override fun equals(other: Any?): Boolean = other is ScriptDependenciesInfo && this.project == other.project

    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.LIBRARY

    override val sourcesModuleInfo: SourceForBinaryModuleInfo?
        get() = ScriptDependenciesSourceInfo.ForProject(project)

    override val platform: TargetPlatform
        get() = JvmPlatforms.unspecifiedJvmPlatform // TODO(dsavvinov): choose proper TargetVersion

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = JvmPlatformAnalyzerServices

    class ForFile(
        project: Project,
        val scriptFile: VirtualFile,
        val scriptDefinition: ScriptDefinition
    ) : ScriptDependenciesInfo(project) {
        override val sdk: Sdk?
            get() {
                return ScriptDependenciesManager.getInstance(project).getScriptSdk(scriptFile)
                    ?: ScriptDependenciesManager.getScriptDefaultSdk(project)
            }

        override fun contentScope(): GlobalSearchScope {
            // TODO: this is not very efficient because KotlinSourceFilterScope already checks if the files are in scripts classpath
            return KotlinSourceFilterScope.libraryClassFiles(
                ScriptDependenciesManager.getInstance(project).getScriptDependenciesClassFilesScope(scriptFile), project
            )
        }
    }

    // we do not know which scripts these dependencies are
    class ForProject(project: Project) : ScriptDependenciesInfo(project) {
        override val sdk: Sdk?
            get() {
                return ScriptDependenciesManager.getInstance(project).getAllScriptsSdks().firstOrNull()
                    ?: ScriptDependenciesManager.getScriptDefaultSdk(project)
            }

        override fun contentScope(): GlobalSearchScope {
            return KotlinSourceFilterScope.libraryClassFiles(
                ScriptDependenciesManager.getInstance(project).getAllScriptsDependenciesClassFilesScope(), project
            )
        }
    }
}

sealed class ScriptDependenciesSourceInfo(val project: Project) : IdeaModuleInfo, SourceForBinaryModuleInfo {
    override val name = Name.special("<Source for script dependencies>")

    override val binariesModuleInfo: ScriptDependenciesInfo
        get() = ScriptDependenciesInfo.ForProject(project)

    override fun sourceScope(): GlobalSearchScope = KotlinSourceFilterScope.librarySources(
        ScriptDependenciesManager.getInstance(project).getAllScriptDependenciesSourcesScope(), project
    )

    override fun hashCode() = project.hashCode()

    override fun equals(other: Any?): Boolean = other is ScriptDependenciesSourceInfo && this.project == other.project

    override val platform: TargetPlatform
        get() = JvmPlatforms.unspecifiedJvmPlatform // TODO(dsavvinov): choose proper TargetVersion

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = JvmPlatformAnalyzerServices

    class ForProject(project: Project) : ScriptDependenciesSourceInfo(project)
}