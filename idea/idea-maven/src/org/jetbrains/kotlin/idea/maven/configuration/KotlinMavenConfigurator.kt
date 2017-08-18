/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.maven.configuration

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.ide.actions.OpenFileAction
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.WritingAccessProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenArtifactScope
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CoroutineSupport
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.facet.getRuntimeLibraryVersion
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion
import org.jetbrains.kotlin.idea.maven.*
import org.jetbrains.kotlin.idea.quickfix.ChangeCoroutineSupportFix
import org.jetbrains.kotlin.idea.util.application.runReadAction

abstract class KotlinMavenConfigurator
        protected constructor(private val testArtifactId: String?,
                              private val addJunit: Boolean,
                              override val name: String,
                              override val presentableText: String) : KotlinProjectConfigurator {

    override fun getStatus(moduleSourceRootGroup: ModuleSourceRootGroup): ConfigureKotlinStatus {
        val module = moduleSourceRootGroup.baseModule
        if (!KotlinPluginUtil.isMavenModule(module))
            return ConfigureKotlinStatus.NON_APPLICABLE

        val psi = runReadAction { findModulePomFile(module) }
        if (psi == null
            || !psi.isValid
            || psi !is XmlFile
            || psi.virtualFile == null) {
            return ConfigureKotlinStatus.BROKEN
        }

        if (isKotlinModule(module)) {
            return runReadAction { checkKotlinPlugin(module) }
        }
        return ConfigureKotlinStatus.CAN_BE_CONFIGURED
    }

    private fun checkKotlinPlugin(module: Module): ConfigureKotlinStatus {
        val psi = findModulePomFile(module) as? XmlFile ?: return ConfigureKotlinStatus.BROKEN
        val pom = PomFile.forFileOrNull(psi) ?: return ConfigureKotlinStatus.NON_APPLICABLE

        if (hasKotlinPlugin(pom)) {
            return ConfigureKotlinStatus.CONFIGURED
        }

        val mavenProjectsManager = MavenProjectsManager.getInstance(module.project)
        val mavenProject = mavenProjectsManager.findProject(module) ?: return ConfigureKotlinStatus.BROKEN

        val kotlinPluginId = kotlinPluginId(null)
        val kotlinPlugin = mavenProject.plugins.find { it.mavenId.equals(kotlinPluginId.groupId, kotlinPluginId.artifactId)}
                           ?: return ConfigureKotlinStatus.CAN_BE_CONFIGURED

        if (kotlinPlugin.executions.any { it.goals.any(this::isRelevantGoal) }) {
            return ConfigureKotlinStatus.CONFIGURED
        }

        return ConfigureKotlinStatus.CAN_BE_CONFIGURED
    }

    private fun hasKotlinPlugin(pom: PomFile): Boolean {
        val plugin = pom.findPlugin(kotlinPluginId(null)) ?: return false

        return plugin.executions.executions.any {
            it.goals.goals.any { isRelevantGoal(it.stringValue ?: "") }
        }
    }

    override fun configure(project: Project, excludeModules: Collection<Module>) {
        val dialog = ConfigureDialogWithModulesAndVersion(project, this, excludeModules, getMinimumSupportedVersion())

        dialog.show()
        if (!dialog.isOK) return

        WriteCommandAction.runWriteCommandAction(project) {
            val collector = createConfigureKotlinNotificationCollector(project)
            for (module in excludeMavenChildrenModules(project, dialog.modulesToConfigure)) {
                val file = findModulePomFile(module)
                if (file != null && canConfigureFile(file)) {
                    changePomFile(module, file, dialog.kotlinVersion, collector)
                    OpenFileAction.openFile(file.virtualFile, project)
                }
                else {
                    showErrorMessage(project, "Cannot find pom.xml for module " + module.name)
                }
            }
            collector.showNotification()
        }
    }

    protected open fun getMinimumSupportedVersion() = "1.0.0"

    protected abstract fun isKotlinModule(module: Module): Boolean
    protected abstract fun isRelevantGoal(goalName: String): Boolean

    protected abstract fun createExecutions(pomFile: PomFile, kotlinPlugin: MavenDomPlugin, module: Module)
    protected abstract fun getStdlibArtifactId(module: Module, version: String): String

    fun changePomFile(
            module: Module,
            file: PsiFile,
            version: String,
            collector: NotificationMessageCollector) {

        val virtualFile = file.virtualFile ?: error("Virtual file should exists for psi file " + file.name)
        val domModel = MavenDomUtil.getMavenDomProjectModel(module.project, virtualFile)
        if (domModel == null) {
            showErrorMessage(module.project, null)
            return
        }

        val pom = PomFile.forFileOrNull(file as XmlFile) ?: return
        pom.addProperty(KOTLIN_VERSION_PROPERTY, version)

        pom.addDependency(MavenId(GROUP_ID, getStdlibArtifactId(module, version), "\${$KOTLIN_VERSION_PROPERTY}"), MavenArtifactScope.COMPILE, null, false, null)
        if (testArtifactId != null) {
            pom.addDependency(MavenId(GROUP_ID, testArtifactId, "\${$KOTLIN_VERSION_PROPERTY}"), MavenArtifactScope.TEST, null, false, null)
        }
        if (addJunit) {
            // TODO currently it is always disabled: junit version selection could be shown in the configurator dialog
            pom.addDependency(MavenId("junit", "junit", "4.12"), MavenArtifactScope.TEST, null, false, null)
        }

        val repositoryDescription = getRepositoryForVersion(version)
        if (repositoryDescription != null) {
            pom.addLibraryRepository(repositoryDescription)
            pom.addPluginRepository(repositoryDescription)
        }

        val plugin = pom.addPlugin(MavenId(GROUP_ID, MAVEN_PLUGIN_ID, "\${$KOTLIN_VERSION_PROPERTY}"))
        createExecutions(pom, plugin, module)

        configurePlugin(pom, plugin, module, version)

        CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement<PsiFile>(file)

        collector.addMessage(virtualFile.path + " was modified")
    }

    protected open fun configurePlugin(pom: PomFile, plugin: MavenDomPlugin, module: Module, version: String) {
    }

    protected fun createExecution(
            pomFile: PomFile,
            kotlinPlugin: MavenDomPlugin,
            executionId: String,
            goalName: String,
            module: Module,
            isTest: Boolean) {

        pomFile.addKotlinExecution(module, kotlinPlugin, executionId, PomFile.getPhase(false, isTest), isTest, listOf(goalName))

        if (hasJavaFiles(module)) {
            pomFile.addJavacExecutions(module, kotlinPlugin)
        }
    }

    override fun updateLanguageVersion(
            module: Module,
            languageVersion: String?,
            apiVersion: String?,
            requiredStdlibVersion: ApiVersion,
            forTests: Boolean
    ) {
        fun doUpdateMavenLanguageVersion(): PsiElement? {
            val psi = KotlinMavenConfigurator.findModulePomFile(module) as? XmlFile ?: return null
            val pom = PomFile.forFileOrNull(psi) ?: return null
            return pom.changeLanguageVersion(
                    languageVersion,
                    apiVersion)
        }

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

        val element = doUpdateMavenLanguageVersion()
        if (element == null) {
            Messages.showErrorDialog(module.project,
                                     "Failed to update.pom.xml. Please update the file manually.",
                                     "Update Language Version")
        }
        else {
            OpenFileDescriptor(module.project, element.containingFile.virtualFile, element.textRange.startOffset).navigate(true)
        }
    }

    override fun changeCoroutineConfiguration(module: Module, state: LanguageFeature.State) {
        val runtimeUpdateRequired = state != LanguageFeature.State.DISABLED &&
                                    (getRuntimeLibraryVersion(module)?.startsWith("1.0") ?: false)

        val messageTitle = ChangeCoroutineSupportFix.getFixText(state)
        if (runtimeUpdateRequired) {
            Messages.showErrorDialog(module.project,
                                     "Coroutines support requires version 1.1 or later of the Kotlin runtime library. " +
                                     "Please update the version in your build script.",
                                     messageTitle)
            return
        }

        val element = changeMavenCoroutineConfiguration(module, CoroutineSupport.getCompilerArgument(state), messageTitle)

        if (element != null) {
            OpenFileDescriptor(module.project, element.containingFile.virtualFile, element.textRange.startOffset).navigate(true)
        }

    }

    private fun changeMavenCoroutineConfiguration(module: Module, value: String, messageTitle: String): PsiElement? {
        fun doChangeMavenCoroutineConfiguration(): PsiElement? {
            val psi = KotlinMavenConfigurator.findModulePomFile(module) as? XmlFile ?: return null
            val pom = PomFile.forFileOrNull(psi) ?: return null
            return pom.changeCoroutineConfiguration(value)
        }

        val element = doChangeMavenCoroutineConfiguration()
        if (element == null) {
            Messages.showErrorDialog(module.project,
                                     "Failed to update.pom.xml. Please update the file manually.",
                                     messageTitle)
        }
        return element
    }

    companion object {
        val NAME = "maven"

        val GROUP_ID = "org.jetbrains.kotlin"
        val MAVEN_PLUGIN_ID = "kotlin-maven-plugin"
        private val KOTLIN_VERSION_PROPERTY = "kotlin.version"

        private fun hasJavaFiles(module: Module): Boolean {
            return !FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.moduleScope(module)).isEmpty()
        }

        fun findModulePomFile(module: Module): PsiFile? {
            val files = MavenProjectsManager.getInstance(module.project).projectsFiles
            for (file in files) {
                val fileModule = ModuleUtilCore.findModuleForFile(file, module.project)
                if (module != fileModule) continue
                val psiFile = PsiManager.getInstance(module.project).findFile(file) ?: continue
                if (!MavenDomUtil.isProjectFile(psiFile)) continue
                return psiFile
            }
            return null
        }

        private fun canConfigureFile(file: PsiFile): Boolean {
            return WritingAccessProvider.isPotentiallyWritable(file.virtualFile, null)
        }

        private fun showErrorMessage(project: Project, message: String?) {
            Messages.showErrorDialog(project,
                                     "<html>Couldn't configure kotlin-maven plugin automatically.<br/>" +
                                     (if (message != null) "$message</br>" else "") +
                                     "See manual installation instructions <a href=\"http://confluence.jetbrains.com/display/Kotlin/Kotlin+Build+Tools#KotlinBuildTools-Maven\">here</a>.</html>",
                                     "Configure Kotlin-Maven Plugin")
        }
    }
}
