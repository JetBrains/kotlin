/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.WritingAccessProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CoroutineSupport
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.facet.getRuntimeLibraryVersion
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion
import org.jetbrains.kotlin.idea.quickfix.ChangeCoroutineSupportFix
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor
import org.jetbrains.kotlin.idea.versions.getStdlibArtifactId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import java.io.File
import java.util.*

abstract class KotlinWithGradleConfigurator : KotlinProjectConfigurator {

    override fun getStatus(moduleSourceRootGroup: ModuleSourceRootGroup): ConfigureKotlinStatus {
        val module = moduleSourceRootGroup.baseModule
        if (!isApplicable(module)) {
            return ConfigureKotlinStatus.NON_APPLICABLE
        }

        if (moduleSourceRootGroup.sourceRootModules.all(::hasAnyKotlinRuntimeInScope)) {
            return ConfigureKotlinStatus.CONFIGURED
        }

        val buildFiles = runReadAction {
            listOf(
                    module.getBuildScriptPsiFile(),
                    module.project.getTopLevelBuildScriptPsiFile()
            ).filterNotNull()
        }

        if (buildFiles.isEmpty()) {
            return ConfigureKotlinStatus.NON_APPLICABLE
        }

        if (buildFiles.none { it.isConfiguredByAnyGradleConfigurator() }) {
            return ConfigureKotlinStatus.CAN_BE_CONFIGURED
        }

        return ConfigureKotlinStatus.BROKEN
    }

    private fun PsiFile.isConfiguredByAnyGradleConfigurator(): Boolean {
        return Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)
                .filterIsInstance<KotlinWithGradleConfigurator>()
                .any { it.isFileConfigured(this) }
    }

    protected open fun isApplicable(module: Module): Boolean =
            KotlinPluginUtil.isGradleModule(module) && !KotlinPluginUtil.isAndroidGradleModule(module)

    protected open fun getMinimumSupportedVersion() = "1.0.0"

    private fun isFileConfigured(buildScript: PsiFile): Boolean = getManipulator(buildScript).isConfigured(kotlinPluginName)

    @JvmSuppressWildcards
    override fun configure(project: Project, excludeModules: Collection<Module>) {
        val dialog = ConfigureDialogWithModulesAndVersion(project, this, excludeModules, getMinimumSupportedVersion())

        dialog.show()
        if (!dialog.isOK) return

        val collector = configureSilently(project, dialog.modulesToConfigure, dialog.kotlinVersion)
        collector.showNotification()
    }

    fun configureSilently(project: Project, modules: List<Module>, version: String): NotificationMessageCollector {
        return project.executeCommand("Configure Kotlin") {
            val collector = createConfigureKotlinNotificationCollector(project)
            val changedFiles = configureWithVersion(project, modules, version, collector)

            for (file in changedFiles) {
                OpenFileAction.openFile(file.virtualFile, project)
            }
            collector
        }
    }

    fun configureWithVersion(project: Project,
                             modulesToConfigure: List<Module>,
                             kotlinVersion: String,
                             collector: NotificationMessageCollector): HashSet<PsiFile> {
        val filesToOpen = HashSet<PsiFile>()
        val buildScript = project.getTopLevelBuildScriptPsiFile()
        if (buildScript != null && canConfigureFile(buildScript)) {
            val isModified = configureBuildScript(buildScript, true, kotlinVersion, collector)
            if (isModified) {
                filesToOpen.add(buildScript)
            }
        }

        for (module in modulesToConfigure) {
            val file = module.getBuildScriptPsiFile()
            if (file != null && canConfigureFile(file)) {
                configureModule(module, file, false, kotlinVersion, collector, filesToOpen)
            }
            else {
                showErrorMessage(project, "Cannot find build.gradle file for module " + module.name)
            }
        }
        return filesToOpen
    }

    open fun configureModule(
            module: Module,
            file: PsiFile,
            isTopLevelProjectFile: Boolean,
            version: String,
            collector: NotificationMessageCollector,
            filesToOpen: MutableCollection<PsiFile>
    ) {
        val isModified = configureBuildScript(file, isTopLevelProjectFile, version, collector)
        if (isModified) {
            filesToOpen.add(file)
        }
    }

    protected fun configureModuleBuildScript(file: PsiFile, version: String): Boolean {
        val sdk = ModuleUtil.findModuleForPsiElement(file)?.let { ModuleRootManager.getInstance(it).sdk }
        val jvmTarget = getJvmTarget(sdk, version)
        return getManipulator(file).configureModuleBuildScript(
                kotlinPluginName,
                getStdlibArtifactName(sdk, version),
                version,
                jvmTarget)
    }

    protected open fun getStdlibArtifactName(sdk: Sdk?, version: String) = getStdlibArtifactId(sdk, version)

    protected open fun getJvmTarget(sdk: Sdk?, version: String): String? = null

    protected abstract val kotlinPluginName: String

    protected open fun addElementsToFile(
            file: PsiFile,
            isTopLevelProjectFile: Boolean,
            version: String
    ): Boolean {
        if (!isTopLevelProjectFile) {
            var wasModified = configureProjectFile(file, version)
            wasModified = wasModified or configureModuleBuildScript(file, version)
            return wasModified
        }
        return false
    }

    private fun configureBuildScript(
            file: PsiFile,
            isTopLevelProjectFile: Boolean,
            version: String,
            collector: NotificationMessageCollector
    ): Boolean {
        val isModified = file.project.executeWriteCommand("Configure ${file.name}", null) {
            val isModified = addElementsToFile(file, isTopLevelProjectFile, version)

            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(file)
            isModified
        }

        val virtualFile = file.virtualFile
        if (virtualFile != null && isModified) {
            collector.addMessage(virtualFile.path + " was modified")
        }
        return isModified
    }

    override fun updateLanguageVersion(
            module: Module,
            languageVersion: String?,
            apiVersion: String?,
            requiredStdlibVersion: ApiVersion,
            forTests: Boolean
    ) {
        val runtimeUpdateRequired = getRuntimeLibraryVersion(module)?.let { ApiVersion.parse(it) }?.let { runtimeVersion ->
            runtimeVersion < requiredStdlibVersion
        } ?: false

        if (runtimeUpdateRequired) {
            Messages.showErrorDialog(module.project,
                                     "This language feature requires version $requiredStdlibVersion or later of the Kotlin runtime library. " +
                                     "Please update the version in your build script.",
                                     "Update Language Version")
            return
        }

        val element = changeLanguageVersion(module, languageVersion, apiVersion, forTests)

        element?.let {
            OpenFileDescriptor(module.project, it.containingFile.virtualFile, it.textRange.startOffset).navigate(true)
        }
    }

    override fun changeCoroutineConfiguration(module: Module, state: LanguageFeature.State) {
        val runtimeUpdateRequired = state != LanguageFeature.State.DISABLED &&
                                    (getRuntimeLibraryVersion(module)?.startsWith("1.0") ?: false)

        if (runtimeUpdateRequired) {
            Messages.showErrorDialog(module.project,
                                     "Coroutines support requires version 1.1 or later of the Kotlin runtime library. " +
                                     "Please update the version in your build script.",
                                     ChangeCoroutineSupportFix.getFixText(state))
            return
        }

        val element = changeCoroutineConfiguration(module, CoroutineSupport.getCompilerArgument(state))
        if (element != null) {
            OpenFileDescriptor(module.project, element.containingFile.virtualFile, element.textRange.startOffset).navigate(true)
        }
    }

    override fun addLibraryDependency(module: Module, element: PsiElement, library: ExternalLibraryDescriptor, libraryJarDescriptors: List<LibraryJarDescriptor>) {
        val scope = OrderEntryFix.suggestScopeByLocation(module, element)
        KotlinWithGradleConfigurator.addKotlinLibraryToModule(module, scope, library)
    }

    companion object {
        fun getManipulator(file: PsiFile): GradleBuildScriptManipulator = when (file) {
            is KtFile -> KotlinBuildScriptManipulator(file)
            is GroovyFile -> GroovyBuildScriptManipulator(file)
            else -> error("Unknown build script file type!")
        }

        val GROUP_ID = "org.jetbrains.kotlin"
        val GRADLE_PLUGIN_ID = "kotlin-gradle-plugin"

        val CLASSPATH = "classpath \"$GROUP_ID:$GRADLE_PLUGIN_ID:\$kotlin_version\""

        private val KOTLIN_BUILD_SCRIPT_NAME = "build.gradle.kts"

        fun getGroovyDependencySnippet(artifactName: String, scope: String = "compile") =
                "$scope \"org.jetbrains.kotlin:$artifactName:\$kotlin_version\""

        fun getGroovyApplyPluginDirective(pluginName: String) = "apply plugin: '$pluginName'"

        fun addKotlinLibraryToModule(module: Module, scope: DependencyScope, libraryDescriptor: ExternalLibraryDescriptor) {
            val buildScript = module.getBuildScriptPsiFile() ?: return
            if (!canConfigureFile(buildScript)) {
                return
            }

            getManipulator(buildScript)
                    .addKotlinLibraryToModuleBuildScript(scope, libraryDescriptor, KotlinPluginUtil.isAndroidGradleModule(module))

            buildScript.virtualFile?.let {
                createConfigureKotlinNotificationCollector(buildScript.project)
                        .addMessage(it.path + " was modified")
                        .showNotification()
            }
        }

        fun changeCoroutineConfiguration(module: Module, coroutineOption: String): PsiElement? = changeBuildGradle(module) {
            getManipulator(it).changeCoroutineConfiguration(coroutineOption)
        }

        fun changeLanguageVersion(module: Module, languageVersion: String?, apiVersion: String?, forTests: Boolean) =
                changeBuildGradle(module) { buildScriptFile ->
                    val manipulator = getManipulator(buildScriptFile)
                    var result: PsiElement? = null
                    if (languageVersion != null) {
                        result = manipulator.changeLanguageVersion(languageVersion, forTests)
                    }

                    if (apiVersion != null) {
                        result = manipulator.changeApiVersion(apiVersion, forTests)
                    }

                    result
                }

        private fun changeBuildGradle(module: Module, body: (PsiFile) -> PsiElement?): PsiElement? {
            val buildScriptFile = module.getBuildScriptPsiFile()
            if (buildScriptFile != null && canConfigureFile(buildScriptFile)) {
                return buildScriptFile.project.executeWriteCommand("Change build.gradle configuration", null) {
                    body(buildScriptFile)
                }
            }
            return null
        }

        fun getKotlinStdlibVersion(module: Module): String? {
            return module.getBuildScriptPsiFile()?.let {
                getManipulator(it).getKotlinStdlibVersion()
            }
        }

        fun configureProjectFile(file: PsiFile, version: String): Boolean = getManipulator(file).configureProjectBuildScript(version)

        private fun canConfigureFile(file: PsiFile): Boolean = WritingAccessProvider.isPotentiallyWritable(file.virtualFile, null)

        private fun Module.getBuildScriptPsiFile() = getBuildScriptFile()?.getPsiFile(project)

        private fun Project.getTopLevelBuildScriptPsiFile() = basePath?.let { findBuildGradleFile(it)?.getPsiFile(this) }

        private fun Module.getBuildScriptFile(): File? {
            val moduleDir = File(moduleFilePath).parent
            findBuildGradleFile(moduleDir)?.let {
                return it
            }

            ModuleRootManager.getInstance(this).contentRoots.forEach { root ->
                findBuildGradleFile(root.path)?.let {
                    return it
                }
            }

            ExternalSystemApiUtil.getExternalProjectPath(this)?.let { externalProjectPath ->
                findBuildGradleFile(externalProjectPath)?.let {
                    return it
                }
            }

            return null
        }

        private fun findBuildGradleFile(path: String): File? =
                File(path + "/" + GradleConstants.DEFAULT_SCRIPT_NAME).takeIf { it.exists() } ?:
                File(path + "/" + KOTLIN_BUILD_SCRIPT_NAME).takeIf { it.exists() }

        private fun File.getPsiFile(project: Project) = VfsUtil.findFileByIoFile(this, true)?.let {
            PsiManager.getInstance(project).findFile(it)
        }

        private fun showErrorMessage(project: Project, message: String?) {
            Messages.showErrorDialog(project,
                                     "<html>Couldn't configure kotlin-gradle plugin automatically.<br/>" +
                                     (if (message != null) message + "<br/>" else "") +
                                     "<br/>See manual installation instructions <a href=\"https://kotlinlang.org/docs/reference/using-gradle.html\">here</a>.</html>",
                                     "Configure Kotlin-Gradle Plugin")
        }
    }
}
