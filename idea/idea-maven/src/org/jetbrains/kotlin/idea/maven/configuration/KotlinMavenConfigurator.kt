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
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.WritingAccessProvider
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
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.configuration.NotificationMessageCollector
import org.jetbrains.kotlin.idea.configuration.createConfigureKotlinNotificationCollector
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.maven.excludeMavenChildrenModules

abstract class KotlinMavenConfigurator protected constructor(private val stdlibArtifactId: String, private val testArtifactId: String?, private val addJunit: Boolean, private val name: String, private val presentableText: String) : KotlinProjectConfigurator {

    override fun isApplicable(module: Module): Boolean {
        return KotlinPluginUtil.isMavenModule(module)
    }

    override fun getPresentableText(): String {
        return presentableText
    }

    override fun getName(): String {
        return name
    }

    override fun isConfigured(module: Module): Boolean {
        if (!isKotlinModule(module)) {
            return false
        }

        val psi = findModulePomFile(module)
        if (psi == null
            || !psi.isValid
            || psi !is XmlFile
            || psi.virtualFile == null
            || MavenDomUtil.getMavenDomProjectModel(module.project, psi.virtualFile) == null) {
            return false
        }

        val mavenProject = MavenProjectsManager.getInstance(module.project).findProject(module) ?: return false

        val plugin = mavenProject.findPlugin(GROUP_ID, MAVEN_PLUGIN_ID) ?: return false

        if (plugin.executions != null) {
            for (execution in plugin.executions) {
                if (execution.goals != null) {
                    for (goal in execution.goals) {
                        if (goal != null && isRelevantGoal(goal)) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    override fun configure(project: Project, excludeModules: Collection<Module>) {
        val dialog = ConfigureDialogWithModulesAndVersion(project, this, excludeModules)

        dialog.show()
        if (!dialog.isOK) return

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

    protected abstract fun isKotlinModule(module: Module): Boolean
    protected abstract fun isRelevantGoal(goalName: String): Boolean

    protected abstract fun createExecutions(pomFile: PomFile, kotlinPlugin: MavenDomPlugin, module: Module)

    protected abstract fun getGoal(isTest: Boolean): String

    protected open fun getExecutionId(isTest: Boolean): String {
        return if (isTest) TEST_COMPILE_EXECUTION_ID else COMPILE_EXECUTION_ID
    }

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

        WriteCommandAction.runWriteCommandAction(module.project) {
            val pom = PomFile(file as XmlFile)
            pom.addProperty(KOTLIN_VERSION_PROPERTY, version)

            pom.addDependency(MavenId(GROUP_ID, stdlibArtifactId, "\${$KOTLIN_VERSION_PROPERTY}"), MavenArtifactScope.COMPILE, null, false, null)
            if (testArtifactId != null) {
                pom.addDependency(MavenId(GROUP_ID, testArtifactId, "\${$KOTLIN_VERSION_PROPERTY}"), MavenArtifactScope.TEST, null, false, null)
            }
            if (addJunit) {
                pom.addDependency(MavenId("junit", "junit", "4.12"), MavenArtifactScope.TEST, null, false, null)
            }

            if (isSnapshot(version)) {
                pom.addLibraryRepository(SNAPSHOT_REPOSITORY_ID, SONATYPE_OSS_REPOSITORY_NAME, SONATYPE_OSS_REPOSITORY_URL, true, false)
                pom.addPluginRepository(SNAPSHOT_REPOSITORY_ID, SONATYPE_OSS_REPOSITORY_NAME, SONATYPE_OSS_REPOSITORY_URL, true, false)
            }

            val plugin = pom.addPlugin(MavenId(GROUP_ID, MAVEN_PLUGIN_ID, "\${$KOTLIN_VERSION_PROPERTY}"))
            createExecutions(pom, plugin, module)

            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement<PsiFile>(file)
        }

        collector.addMessage(virtualFile.path + " was modified")
    }

    protected fun createExecution(
            pomFile: PomFile,
            kotlinPlugin: MavenDomPlugin,
            module: Module,
            isTest: Boolean) {
        pomFile.addKotlinExecution(module, kotlinPlugin, getExecutionId(isTest), PomFile.getPhase(hasJavaFiles(module), isTest), isTest,
                                   listOf(getGoal(isTest)))
    }

    companion object {
        val NAME = "maven"

        val GROUP_ID = "org.jetbrains.kotlin"
        val MAVEN_PLUGIN_ID = "kotlin-maven-plugin"
        private val KOTLIN_VERSION_PROPERTY = "kotlin.version"
        private val SNAPSHOT_REPOSITORY_ID = "sonatype.oss.snapshots"
        private val SONATYPE_OSS_REPOSITORY_NAME = "Sonatype OSS Snapshot Repository"
        private val SONATYPE_OSS_REPOSITORY_URL = "http://oss.sonatype.org/content/repositories/snapshots"

        private val TEST_COMPILE_EXECUTION_ID = "test-compile"
        private val COMPILE_EXECUTION_ID = "compile"

        private fun hasJavaFiles(module: Module): Boolean {
            return !FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.moduleScope(module)).isEmpty()
        }

        private fun findModulePomFile(module: Module): PsiFile? {
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

        private fun isSnapshot(version: String): Boolean {
            return version.contains("SNAPSHOT")
        }

        private fun canConfigureFile(file: PsiFile): Boolean {
            return WritingAccessProvider.isPotentiallyWritable(file.virtualFile, null)
        }

        private fun showErrorMessage(project: Project, message: String?) {
            Messages.showErrorDialog(project,
                                     "<html>Couldn't configure kotlin-maven plugin automatically.<br/>" +
                                     (message ?: "") +
                                     "See manual installation instructions <a href=\"http://confluence.jetbrains.com/display/Kotlin/Kotlin+Build+Tools#KotlinBuildTools-Maven\">here</a></html>",
                                     "Configure Kotlin-Maven Plugin")
        }
    }
}
