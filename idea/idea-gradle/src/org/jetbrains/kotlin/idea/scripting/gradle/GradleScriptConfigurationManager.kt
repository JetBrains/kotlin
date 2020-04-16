/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupportHelper
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsIndexer
import org.jetbrains.kotlin.idea.scripting.gradle.importing.GradleKtsContext
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.idea.scripting.gradle.importing.toScriptConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

internal data class ConfigurationData(
    val templateClasspath: List<String>,
    val models: List<KotlinDslScriptModel>
)

internal class Configuration(val data: ConfigurationData) {
    private val scripts: Map<String, KotlinDslScriptModel>

    val sourcePath: MutableSet<String>
    val classFilePath: MutableSet<String> = mutableSetOf()

    init {
        val allModels = data.models

        scripts = allModels.associateBy { it.file }
        sourcePath = allModels.flatMapTo(mutableSetOf()) { it.sourcePath }

        classFilePath.addAll(data.templateClasspath)
        allModels.flatMapTo(classFilePath) { it.classPath }
    }

    fun scriptModel(file: VirtualFile): KotlinDslScriptModel? {
        return scripts[FileUtil.toSystemDependentName(file.path)]
    }
}

internal class GradleScriptingSupport(
    val rootsIndexer: ScriptClassRootsIndexer,
    val project: Project,
    val buildRoot: VirtualFile,
    val context: GradleKtsContext,
    configuration: Configuration?
) : ScriptingSupport() {
    class Provider(val project: Project) : ScriptingSupport.Provider() {
        val rootsIndexer = ScriptClassRootsIndexer(project)

        override var all: List<GradleScriptingSupport> = listOf()

        init {
            reloadSettings()
        }

        fun reloadSettings() {
            val outdated = all.associateByTo(mutableMapOf()) { it.buildRoot }
            val all = mutableListOf<GradleScriptingSupport>()

            getGradleProjectSettings(project).forEach {
                val vf = VfsUtil.findFile(Paths.get(it.externalProjectPath), true)
                if (vf != null) {
                    val gradleExeSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
                        project,
                        it.externalProjectPath,
                        GradleConstants.SYSTEM_ID
                    )

                    val javaHome = gradleExeSettings.javaHome

                    if (javaHome != null) {
                        val context = GradleKtsContext(File(javaHome))

                        val old = outdated.remove(vf)
                        val oldConfiguration = old?.configuration?.get()
                        all.add(GradleScriptingSupport(rootsIndexer, project, vf, context, oldConfiguration))
                    }
                }
            }

            this.all = all

            outdated.forEach {
                it.value.dispose()
            }
        }

        override fun getSupport(file: VirtualFile): ScriptingSupport? =
            all.find {
                file.path.startsWith(it.buildRoot.path)
            }

        companion object {
            fun getInstance(project: Project): Provider =
                EPN.getPoint(project).extensionList.firstIsInstance()
        }
    }

    private val configuration = AtomicReference<Configuration?>(configuration)

    init {
        if (configuration == null) {
            if (isKotlinDslScriptsModelImportSupported(project)) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    val data = KotlinDslScriptModels.read(buildRoot)
                    if (data != null) {
                        compareAndSetConfiguration(null, Configuration(data))
                    }
                }
            }
        }
    }

    override fun recreateRootsCache(): ScriptClassRootsCache {
        val configuration = configuration.get()
        return GradleClassRootsCache(project, context, configuration) {
            configuration?.scriptModel(it)?.toScriptConfiguration(context, project)
        }
    }

    fun replace(models: List<KotlinDslScriptModel>) {
        val old = configuration.get()
        if (models.isEmpty()) return

        val anyScript = VfsUtil.findFile(Paths.get(models.first().file), true)!!

        val definition = anyScript.findScriptDefinition(project) ?: return
        val templateClasspath = definition.asLegacyOrNull<KotlinScriptDefinitionFromAnnotatedTemplate>()
            ?.templateClasspath?.map { it.path } ?: return

        val data = ConfigurationData(templateClasspath, models)
        val new = Configuration(data)

        if (compareAndSetConfiguration(old, new)) {
            KotlinDslScriptModels.write(buildRoot, data)
        }
    }

    private fun compareAndSetConfiguration(old: Configuration?, new: Configuration): Boolean {
        if (!configuration.compareAndSet(old, new)) return false

        rootsIndexer.transaction {
            if (classpathRoots.hasNotCachedRoots(GradleClassRootsCache.extractRoots(context, new))) {
                rootsIndexer.markNewRoot()
            }

            clearClassRootsCaches(project)

            ScriptingSupportHelper.updateHighlighting(project) {
                new.scriptModel(it) != null
            }
        }

        hideNotificationForProjectImport(project)

        return true
    }

    fun updateNotification(file: KtFile) {
        val vFile = file.originalFile.virtualFile
        val scriptModel = configuration.get()?.scriptModel(vFile) ?: return

        if (scriptModel.inputs.isUpToDate(project, vFile)) {
            hideNotificationForProjectImport(project)
        } else {
            showNotificationForProjectImport(project)
        }
    }

    private fun isKotlinDslScriptsModelImportSupported(project: Project): Boolean {
        val gradleVersion = getGradleVersion(project)
        return gradleVersion != null && kotlinDslScriptsModelImportSupported(gradleVersion)
    }

    override fun clearCaches() {
        // todo should clear up to date
    }

    override fun hasCachedConfiguration(file: KtFile): Boolean =
        configuration.get()?.scriptModel(file.originalFile.virtualFile) != null

    override fun getOrLoadConfiguration(virtualFile: VirtualFile, preloadedKtFile: KtFile?): ScriptCompilationConfigurationWrapper? {
        val configuration = configuration
        if (configuration.get() == null) {
            // todo: show notification "Import gradle project"
            return null
        } else {
            return classpathRoots.getScriptConfiguration(virtualFile)
        }
    }

    override val updater: ScriptConfigurationUpdater
        get() = object : ScriptConfigurationUpdater {
            override fun ensureUpToDatedConfigurationSuggested(file: KtFile) {
                // do nothing for gradle scripts
            }

            // unused symbol inspection should not initiate loading
            override fun ensureConfigurationUpToDate(files: List<KtFile>): Boolean = true

            override fun suggestToUpdateConfigurationIfOutOfDate(file: KtFile) {
                updateNotification(file)
            }
        }

    private fun dispose() {
        KotlinDslScriptModels.remove(buildRoot)
    }
}