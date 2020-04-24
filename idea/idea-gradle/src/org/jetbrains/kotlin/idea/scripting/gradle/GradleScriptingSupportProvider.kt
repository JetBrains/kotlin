/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.CompositeScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupportHelper
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslGradleBuildSync
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Paths

/**
 * Creates [GradleScriptingSupport] per each linked Gradle build.
 * Gradle project sync should call [update].
 */
class GradleScriptingSupportProvider(val project: Project) : ScriptingSupport.Provider() {
    val manager: CompositeScriptConfigurationManager
        get() = TODO()

    private val updater = manager.rootsUpdater

    val roots = RootsIndex<GradleBuildRoot>()

    private val VirtualFile.localPath
        get() = path

    fun findRoot(file: VirtualFile): GradleBuildRoot? {
        val found = roots.findRoot(file.localPath)
        if (found != null) return found

        val settings = findExternalProjectSettings(file) ?: return detectUnlinkedGradleBuildRoot(file)
        val supported = kotlinDslScriptsModelImportSupported(settings.resolveGradleVersion().toString())
        if (supported) {
            return GradleBuildRoot.New()
        } else {
            return GradleBuildRoot.Legacy()
        }
    }

    private fun findExternalProjectSettings(file: VirtualFile): GradleProjectSettings? =
        getGradleProjectSettings(project)
            .filter { file.localPath.startsWith(it.externalProjectPath) }
            .maxBy { it.externalProjectPath.length }

    private fun detectUnlinkedGradleBuildRoot(file: VirtualFile): GradleBuildRoot.Unlinked? = null

    override fun clearCaches() {
        // todo: clear up to date
    }

    override fun isConfigurationLoadingInProgress(file: KtFile): Boolean {
        return when (val root = findRoot(file.originalFile.virtualFile)) {
            is GradleBuildRoot.Linked -> root.importing
            else -> false
        }
    }

    override fun collectConfigurations(builder: ScriptClassRootsCache.Builder) {
        roots.values.forEach { root ->
            if (root is GradleBuildRoot.Imported) {
                root.collectConfigurations(builder)
            }
        }
    }

    fun getScriptInfo(file: VirtualFile): GradleBuildRoot.Imported.ScriptInfo? =
        manager.classpathRoots.getLightScriptInfo(file.localPath) as? GradleBuildRoot.Imported.ScriptInfo

    init {
        ApplicationManager.getApplication().executeOnPooledThread {
            updater.update {
                val roots = mutableListOf<GradleBuildRoot.Imported>()
                getGradleProjectSettings(project).forEach { gradleProjectSettings ->
                    if (kotlinDslScriptsModelImportSupported(getGradleVersion(project, gradleProjectSettings))) {
                        val support = createRoot(gradleProjectSettings.externalProjectPath) {
                            KotlinDslScriptModels.read(it)
                        }

                        if (support != null) {
                            roots.add(support)
                        }
                    }
                }
            }
        }

        // subscribe to gradle build unlink
        val listener = object : GradleSettingsListenerAdapter() {
            override fun onProjectsUnlinked(linkedProjectPaths: MutableSet<String>) {
                linkedProjectPaths.forEach {
                    val buildRoot = VfsUtil.findFile(Paths.get(it), false)
                    if (buildRoot != null) {
                        if (roots.remove(buildRoot.localPath) != null) {
                            KotlinDslScriptModels.remove(buildRoot)
                        }
                    }
                }
            }
        }

        project.messageBus.connect(project).subscribe(GradleSettingsListener.TOPIC, listener)
    }

    fun update(build: KotlinDslGradleBuildSync) {
        // fast path for linked gradle builds without .gradle.kts support
        if (build.models.isEmpty()) {
            val root = roots.findRoot(build.workingDir) ?: return
            if (root is GradleBuildRoot.Imported && root.data.models.isEmpty()) return
        }

        updater.update {
            val templateClasspath = findTemplateClasspath(build) ?: return
            val data = GradleImportedBuildRootData(templateClasspath, build.models)
            val newSupport = createRoot(build.workingDir) { data } ?: return
            KotlinDslScriptModels.write(newSupport.dir, data)
            roots[newSupport.dir.localPath] = newSupport
        }
    }

    private fun createRoot(
        externalProjectPath: String,
        dataProvider: (buildRoot: VirtualFile) -> GradleImportedBuildRootData?
    ): GradleBuildRoot.Imported? {
        updater.invalidate()

        val gradleExeSettings =
            ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
                project,
                externalProjectPath,
                GradleConstants.SYSTEM_ID
            )

        val buildRoot = VfsUtil.findFile(Paths.get(externalProjectPath), true) ?: return null
        val data = dataProvider(buildRoot) ?: return null

        val newSupport = GradleBuildRoot.Imported(
            project,
            buildRoot,
            externalProjectPath,
            GradleKtsContext(gradleExeSettings.javaHome?.let { File(it) }),
            data
        )

        val oldSupport = roots.findRoot(externalProjectPath)
        if (oldSupport != null) {
            val files = newSupport.data.models.mapTo(mutableSetOf()) { it.file }
            ScriptingSupportHelper.updateHighlighting(project) {
                it.path in files
            }

            hideNotificationForProjectImport(project)
        }

        return newSupport
    }

    private fun findTemplateClasspath(build: KotlinDslGradleBuildSync): List<String>? {
        val anyScript = VfsUtil.findFile(Paths.get(build.models.first().file), true)!!
        // todo: find definition according to build.workingDir
        val definition = anyScript.findScriptDefinition(project) ?: return null
        return definition.asLegacyOrNull<KotlinScriptDefinitionFromAnnotatedTemplate>()
            ?.templateClasspath?.map { it.path }
    }

    fun isMissingConfigurationCanBeLoadedDuringImport(file: VirtualFile): Boolean {
        return findRoot(file) is GradleBuildRoot.New
    }

    // used in 201
    @Suppress("UNUSED")
    fun isConfigurationOutOfDate(file: VirtualFile): Boolean {
        val script = getScriptInfo(file) ?: return false
        return script.model.inputs.isUpToDate(project, file)
    }

    companion object {
        fun getInstance(project: Project): GradleScriptingSupportProvider =
            EPN.getPoint(project).extensionList.firstIsInstance()
    }
}