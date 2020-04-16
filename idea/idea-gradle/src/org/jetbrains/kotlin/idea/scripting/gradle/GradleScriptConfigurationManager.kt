/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.application.ApplicationManager
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
import org.jetbrains.kotlin.idea.scripting.gradle.importing.GradleProjectId
import org.jetbrains.kotlin.idea.scripting.gradle.importing.createGradleKtsContextIfPossible
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.idea.scripting.gradle.importing.toScriptConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.nio.file.Paths


internal data class ConfigurationData(
    val templateClasspath: List<String>,
    val models: Map<GradleProjectId, List<KotlinDslScriptModel>>
)

internal class Configuration(val context: GradleKtsContext, val data: ConfigurationData) {
    private val scripts: Map<String, KotlinDslScriptModel>

    val sourcePath: MutableSet<String>
    val classFilePath: MutableSet<String> = mutableSetOf()

    init {
        val allModels: List<KotlinDslScriptModel> = data.models.flatMap { it.value }

        scripts = allModels.associateBy { it.file }
        sourcePath = allModels.flatMapTo(mutableSetOf()) { it.sourcePath }

        classFilePath.addAll(data.templateClasspath)
        allModels.flatMapTo(classFilePath) { it.classPath }
    }

    fun scriptModel(file: VirtualFile): KotlinDslScriptModel? {
        return scripts[FileUtil.toSystemDependentName(file.path)]
    }
}

class GradleScriptingSupport(val project: Project) : ScriptingSupport() {
    @Volatile
    private var configuration: Configuration? = null

    private val rootsIndexer = ScriptClassRootsIndexer(project)

    init {
        if (isKotlinDslScriptsModelImportSupported(project)) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val data = KotlinDslScriptModels.read(project)
                val gradleKtsContext = createGradleKtsContextIfPossible(project)
                if (data != null && gradleKtsContext != null) {
                    val newConfiguration = Configuration(
                        gradleKtsContext,
                        data
                    )

                    configuration = newConfiguration
                    configurationChangedCallback(newConfiguration)
                }
            }
        }
    }

    override fun recreateRootsCache(): ScriptClassRootsCache {
        return GradleClassRootsCache(project, configuration) {
            configuration?.let { conf ->
                val model = conf.scriptModel(it)
                model?.toScriptConfiguration(conf.context, project)
            }
        }
    }

    @Synchronized
    fun replace(context: GradleKtsContext, projectIdToModels: Pair<GradleProjectId, List<KotlinDslScriptModel>>) {
        val models = projectIdToModels.second
        if (models.isEmpty()) return

        val anyScript = VfsUtil.findFile(Paths.get(models.first().file), true)!!

        val definition = anyScript.findScriptDefinition(project) ?: return
        val templateClasspath = definition.asLegacyOrNull<KotlinScriptDefinitionFromAnnotatedTemplate>()
            ?.templateClasspath?.map { it.path } ?: return

        val oldConfiguration = configuration

        @Suppress("IfThenToElvis")
        val newModels = if (oldConfiguration == null) {
            hashMapOf(projectIdToModels)
        } else {
            oldConfiguration.data.models.toMutableMap().apply {
                this[projectIdToModels.first] = models
            }
        }

        val data = ConfigurationData(templateClasspath, newModels)
        val newConfiguration = Configuration(context, data)

        KotlinDslScriptModels.write(project, data)

        configuration = newConfiguration

        configurationChangedCallback(newConfiguration)
    }

    private fun configurationChangedCallback(newConfiguration: Configuration) {
        rootsIndexer.transaction {
            if (classpathRoots.hasNotCachedRoots(GradleClassRootsCache.extractRoots(newConfiguration))) {
                rootsIndexer.markNewRoot()
            }

            clearClassRootsCaches(project)

            ScriptingSupportHelper.updateHighlighting(project) {
                configuration?.scriptModel(it) != null
            }
        }

        hideNotificationForProjectImport(project)
    }

    fun updateNotification(file: KtFile) {
        val vFile = file.originalFile.virtualFile
        val scriptModel = configuration?.scriptModel(vFile) ?: return

        if (scriptModel.inputs.isUpToDate(project, vFile)) {
            hideNotificationForProjectImport(project)
        } else {
            showNotificationForProjectImport(project)
        }
    }

    override fun isRelated(file: VirtualFile): Boolean {
        if (isGradleKotlinScript(file)) {
            return isKotlinDslScriptsModelImportSupported(project)
        }

        return false
    }

    private fun isKotlinDslScriptsModelImportSupported(project: Project): Boolean {
        val gradleVersion = getGradleVersion(project)
        return gradleVersion != null && kotlinDslScriptsModelImportSupported(gradleVersion)
    }

    override fun clearCaches() {
        // todo should clear up to date
    }

    override fun hasCachedConfiguration(file: KtFile): Boolean =
        configuration?.scriptModel(file.originalFile.virtualFile) != null

    override fun getOrLoadConfiguration(virtualFile: VirtualFile, preloadedKtFile: KtFile?): ScriptCompilationConfigurationWrapper? {
        val configuration = configuration
        if (configuration == null) {
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

    companion object {
        fun getInstance(project: Project): GradleScriptingSupport {
            return SCRIPTING_SUPPORT.getPoint(project).extensionList.firstIsInstance()
        }
    }
}