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
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupportHelper
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsIndexer
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslGradleBuildSync
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
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
    private val rootsIndexer = ScriptClassRootsIndexer(project)

    val roots = RootsIndex<GradleScriptingSupport>()
    override val all get() = roots.values

    private val VirtualFile.localPath
        get() = path

    private fun findRoot(file: VirtualFile) = roots.findRoot(file.localPath)

    override fun getSupport(file: VirtualFile): ScriptingSupport? {
        if (isGradleKotlinScript(file)) {
            findRoot(file)?.let { return it }

            val externalProjectSettings = findExternalProjectSettings(file) ?: return null
            if (kotlinDslScriptsModelImportSupported(getGradleVersion(project, externalProjectSettings))) {
                return unlinkedFilesSupport
            }
        }
        return null
    }

    private fun findExternalProjectSettings(file: VirtualFile): GradleProjectSettings? {
        return getGradleProjectSettings(project)
            .filter { file.localPath.startsWith(it.externalProjectPath) }
            .maxBy { it.externalProjectPath.length }
    }

    init {
        ApplicationManager.getApplication().executeOnPooledThread {
            roots.update { set ->
                getGradleProjectSettings(project).forEach { gradleProjectSettings ->
                    if (kotlinDslScriptsModelImportSupported(getGradleVersion(project, gradleProjectSettings))) {
                        val support = createSupport(gradleProjectSettings.externalProjectPath) {
                            KotlinDslScriptModels.read(it)
                        }

                        if (support != null) {
                            set(support.buildRoot.localPath, support)
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
            if (root.configuration.data.models.isEmpty()) return
        }

        val templateClasspath = findTemplateClasspath(build) ?: return
        val data = ConfigurationData(templateClasspath, build.models)
        val newSupport = createSupport(build.workingDir) { data } ?: return
        KotlinDslScriptModels.write(newSupport.buildRoot, data)

        roots[newSupport.buildRoot.localPath] = newSupport
    }

    private fun createSupport(
        externalProjectPath: String,
        dataProvider: (buildRoot: VirtualFile) -> ConfigurationData?
    ): GradleScriptingSupport? {
        val gradleExeSettings =
            ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
                project,
                externalProjectPath,
                GradleConstants.SYSTEM_ID
            )

        val buildRoot = VfsUtil.findFile(Paths.get(externalProjectPath), true) ?: return null
        val data = dataProvider(buildRoot) ?: return null

        val newSupport = GradleScriptingSupport(
            project,
            buildRoot,
            GradleKtsContext(gradleExeSettings.javaHome?.let { File(it) }),
            Configuration(data)
        )

        val oldSupport = roots.findRoot(externalProjectPath)
        if (oldSupport != null) {
            rootsIndexer.transaction {
                val newRoots = GradleClassRootsCache.extractRoots(newSupport.context, newSupport.configuration, project)
                if (oldSupport.classpathRoots.hasNotCachedRoots(newRoots)) {
                    rootsIndexer.markNewRoot()
                }

                newSupport.clearClassRootsCaches(project)

                ScriptingSupportHelper.updateHighlighting(project) {
                    newSupport.configuration.scriptModel(it) != null
                }
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

    private val unlinkedFilesSupport = object : ScriptingSupport() {
        override fun clearCaches() {
        }

        override fun hasCachedConfiguration(file: KtFile): Boolean = false
        override fun isConfigurationLoadingInProgress(file: KtFile): Boolean = false

        override fun getOrLoadConfiguration(
            virtualFile: VirtualFile,
            preloadedKtFile: KtFile?
        ): ScriptCompilationConfigurationWrapper? = null

        override val updater: ScriptConfigurationUpdater
            get() = object : ScriptConfigurationUpdater {
                override fun ensureUpToDatedConfigurationSuggested(file: KtFile) {
                }

                override fun ensureConfigurationUpToDate(files: List<KtFile>): Boolean = false

                override fun suggestToUpdateConfigurationIfOutOfDate(file: KtFile) {
                }
            }

        override fun recreateRootsCache(): ScriptClassRootsCache = ScriptClassRootsCache.empty(project)
    }

    fun isMissingConfigurationCanBeLoadedDuringImport(file: VirtualFile): Boolean {
        findRoot(file)?.let { return false }

        val externalProjectSettings = findExternalProjectSettings(file) ?: return false
        return kotlinDslScriptsModelImportSupported(getGradleVersion(project, externalProjectSettings))
    }

    // used in 201
    @Suppress("UNUSED")
    fun isConfigurationOutOfDate(file: VirtualFile): Boolean {
        val support = findRoot(file) ?: return false
        return support.isConfigurationOutOfDate(file)
    }

    companion object {
        fun getInstance(project: Project): GradleScriptingSupportProvider =
            EPN.getPoint(project).extensionList.firstIsInstance()
    }
}