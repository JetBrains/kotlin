/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.SLRUMap
import com.jetbrains.rd.util.firstOrNull
import org.jetbrains.kotlin.codegen.inline.getOrPut
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.scripting.gradle.importing.GradleKtsContext
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.idea.scripting.gradle.importing.toScriptConfiguration
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.FileSystems

private class Configuration(
    val context: GradleKtsContext,
    models: List<KotlinDslScriptModel>
) {
    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    class Fat(
        val scriptConfiguration: ScriptCompilationConfigurationWrapper,
        val classFilesScope: GlobalSearchScope
    )

    private val memoryCache = SLRUMap<VirtualFile, Fat>(MAX_SCRIPTS_CACHED, MAX_SCRIPTS_CACHED)

    fun String.toVirtualFile() =
        VfsUtil.findFile(FileSystems.getDefault().getPath(this), true)!!

    fun Collection<String>.toVirtualFiles() =
        map { it.toVirtualFile() }

    val sdk = getProjectJdkTableSafe().allJdks.find { it.homeDirectory == context.javaHome }

    val scripts = models.associateBy { it.file }

    val classFiles = models.flatMapTo(mutableSetOf()) { it.classPath }.toVirtualFiles()
    val classFilesScope = NonClasspathDirectoriesScope.compose(classFiles)

    val sources = models.flatMapTo(mutableSetOf()) { it.sourcePath }.toVirtualFiles()
    val sourcesScope = NonClasspathDirectoriesScope.compose(sources)

    operator fun get(key: VirtualFile): Fat? {
        return memoryCache.getOrPut(key) {
            val model = scripts[key.path] ?: return null
            val configuration = model.toScriptConfiguration(context) ?: return null
            Fat(
                configuration,
                NonClasspathDirectoriesScope.compose(
                    if (sdk == null) model.classPath.toVirtualFiles()
                    else sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() + model.classPath.toVirtualFiles()
                )
            )
        }
    }

    private fun computeGradleProjectRoots(project: Project): Set<String> {
        val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        if (gradleSettings.linkedProjectsSettings.isEmpty()) return setOf()

        val projectSettings = gradleSettings.linkedProjectsSettings.filterIsInstance<GradleProjectSettings>().firstOrNull()
            ?: return setOf()

        return projectSettings.modules.takeIf { it.isNotEmpty() } ?: setOf(projectSettings.externalProjectPath)
    }

    // todo: remove it, just return sdk
    fun getAnyLoadedScript() = scripts.firstOrNull()?.key?.let { get(it.toVirtualFile()) }
}

class GradleScriptingSupport(val project: Project) : ScriptingSupport {
    @Volatile
    private var configuration: Configuration? = null

    val file = File("myStorage")

    fun replace(context: GradleKtsContext, models: List<KotlinDslScriptModel>) {
        KotlinDslScriptModels.write(project, models)

        configuration = Configuration(context, models)

        // todo: update unindexed roots
        // todo: remove notification, etc..
    }

    fun load() {
        val gradleProjectSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
            .linkedProjectsSettings
            .filterIsInstance<GradleProjectSettings>().firstOrNull() ?: return

        val javaHome = File(gradleProjectSettings.gradleJvm ?: return)

        val models = KotlinDslScriptModels.read(project) ?: return
        configuration = Configuration(GradleKtsContext(project, javaHome), models)
    }

    init {
        // todo: schedule to background thread
        load()
        // todo: update unindexed roots
        // todo: remove notification, etc..
    }

    override fun isRelated(file: VirtualFile): Boolean {
        if (isGradleKotlinScript(file)) {
            val gradleVersion = getGradleVersion(project)
            if (gradleVersion != null && kotlinDslScriptsModelImportSupported(gradleVersion)) {
                return true
            }
        }

        return false
    }

    override fun clearCaches() {
        configuration = null
    }

    override fun hasCachedConfiguration(file: KtFile): Boolean =
        configuration?.scripts?.containsKey(file.virtualFilePath) ?: false

    override fun getOrLoadConfiguration(virtualFile: VirtualFile, preloadedKtFile: KtFile?): ScriptCompilationConfigurationWrapper? {
        val configuration = configuration
        if (configuration == null) {
            // todo: show notification "Import gradle project"
            return null
        } else {
            return configuration[virtualFile]?.scriptConfiguration
        }
    }

    override fun getAnyLoadedScript() = configuration?.getAnyLoadedScript()?.scriptConfiguration

    // this is not required for gradle in any way
    // unused symbol inspection should not initiate loading
    override val updater: ScriptConfigurationUpdater
        get() = object : ScriptConfigurationUpdater {
            override fun ensureUpToDatedConfigurationSuggested(file: KtFile) = Unit
            override fun ensureConfigurationUpToDate(files: List<KtFile>): Boolean = true
            override fun suggestToUpdateConfigurationIfOutOfDate(file: KtFile) = Unit
        }

    override fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        configuration?.get(file)?.classFilesScope ?: GlobalSearchScope.EMPTY_SCOPE

    override fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope =
        configuration?.classFilesScope ?: GlobalSearchScope.EMPTY_SCOPE

    override fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope =
        configuration?.sourcesScope ?: GlobalSearchScope.EMPTY_SCOPE

    override fun getAllScriptsDependenciesClassFiles(): List<VirtualFile> =
        configuration?.classFiles ?: listOf()

    override fun getAllScriptDependenciesSources(): List<VirtualFile> =
        configuration?.sources ?: listOf()
}