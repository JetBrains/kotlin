/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.run.script.standalone

import com.intellij.execution.*
import com.intellij.execution.application.BaseJavaApplicationCommandLineState
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.execution.util.ProgramParametersUtil
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.DefaultJDOMExternalizer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementAdapter
import com.intellij.refactoring.listeners.RefactoringElementListener
import org.jdom.Element
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.run.script.standalone.KotlinStandaloneScriptRunConfigurationProducer.Companion.pathFromPsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.*

class KotlinStandaloneScriptRunConfiguration(
        project: Project,
        factory: ConfigurationFactory,
        name: String?
) : LocatableConfigurationBase(project, factory, name), CommonJavaRunConfigurationParameters, RefactoringListenerProvider {
    @JvmField
    var filePath: String? = null
    @JvmField
    var vmParameters: String? = null
    @JvmField
    var alternativeJrePath: String? = null
    @JvmField
    var programParameters: String? = null
    @JvmField
    var envs: MutableMap<String, String> = LinkedHashMap()
    @JvmField
    var passParentEnvs: Boolean = true
    @JvmField
    var workingDirectory: String? = null
    @JvmField
    var isAlternativeJrePathEnabled: Boolean = false

    override fun getVMParameters() = vmParameters
    override fun setVMParameters(value: String?) {
        vmParameters = value
    }

    override fun getAlternativeJrePath() = alternativeJrePath
    override fun setAlternativeJrePath(path: String?) {
        alternativeJrePath = path
    }

    override fun getProgramParameters() = programParameters
    override fun setProgramParameters(value: String?) {
        programParameters = value
    }

    override fun getEnvs() = envs
    override fun setEnvs(envs: MutableMap<String, String>) {
        this.envs = envs
    }

    override fun getWorkingDirectory() = workingDirectory
    override fun setWorkingDirectory(value: String?) {
        workingDirectory = value
    }

    override fun isPassParentEnvs() = passParentEnvs
    override fun setPassParentEnvs(passParentEnvs: Boolean) {
        this.passParentEnvs = passParentEnvs
    }

    override fun isAlternativeJrePathEnabled() = isAlternativeJrePathEnabled
    override fun setAlternativeJrePathEnabled(enabled: Boolean) {
        isAlternativeJrePathEnabled = enabled
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = ScriptCommandLineState(environment, this)

    override fun suggestedName() = filePath?.substringAfterLast('/')

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        val group = SettingsEditorGroup<KotlinStandaloneScriptRunConfiguration>()
        group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), KotlinStandaloneScriptRunConfigurationEditor(project))
        JavaRunConfigurationExtensionManager.getInstance().appendEditors(this, group)
        return group
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JavaRunConfigurationExtensionManager.getInstance().writeExternal(this, element)
        DefaultJDOMExternalizer.writeExternal(this, element)
        EnvironmentVariablesComponent.writeExternal(element, getEnvs())
        PathMacroManager.getInstance(project).collapsePathsRecursively(element)
    }

    override fun readExternal(element: Element) {
        PathMacroManager.getInstance(project).expandPaths(element)
        super.readExternal(element)
        JavaRunConfigurationExtensionManager.getInstance().readExternal(this, element)
        DefaultJDOMExternalizer.readExternal(this, element)
        EnvironmentVariablesComponent.readExternal(element, getEnvs())
    }

    override fun checkConfiguration() {
        JavaParametersUtil.checkAlternativeJRE(this)
        ProgramParametersUtil.checkWorkingDirectoryExist(this, project, null)
        JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this)

        if (filePath.isNullOrEmpty()) {
            runtimeConfigurationWarning("File was not specified")
        }
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
        if (virtualFile == null || virtualFile.isDirectory) {
            runtimeConfigurationWarning("Could not find script file: $filePath")
        }
    }

    private fun runtimeConfigurationWarning(message: String): Nothing {
        throw RuntimeConfigurationWarning(message)
    }

    // NOTE: this is needed for coverage
    override fun getRunClass() = null

    override fun getPackage() = null

    override fun getRefactoringElementListener(element: PsiElement): RefactoringElementListener? {
        val file = element as? KtFile ?: return null
        val pathFromElement = pathFromPsiElement(file) ?: return null

        if (filePath != pathFromElement) {
            return null
        }

        return object : RefactoringElementAdapter() {
            override fun undoElementMovedOrRenamed(newElement: PsiElement, oldQualifiedName: String) {
                setupFilePath(pathFromPsiElement(newElement) ?: return)
            }

            override fun elementRenamedOrMoved(newElement: PsiElement) {
                setupFilePath(pathFromPsiElement(newElement) ?: return)
            }
        }
    }

    fun defaultWorkingDirectory(): String? {
        return com.intellij.util.PathUtil.getParentPath(filePath ?: return null)
    }

    fun setupFilePath(filePath: String) {
        val wasDefaultWorkingDirectory = workingDirectory == null || workingDirectory == defaultWorkingDirectory()
        this.filePath = filePath
        if (wasDefaultWorkingDirectory) {
            this.workingDirectory = defaultWorkingDirectory()
        }
    }
}

private class ScriptCommandLineState(
        environment: ExecutionEnvironment,
        configuration: KotlinStandaloneScriptRunConfiguration) :
        BaseJavaApplicationCommandLineState<KotlinStandaloneScriptRunConfiguration>(environment, configuration) {

    override fun createJavaParameters(): JavaParameters? {
        val params = commonParameters()

        val filePath = configuration.filePath ?: throw CantRunException("Script file was not specified")
        val scriptVFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath)) ?:
                          throw CantRunException("Script file was not found in project")

        params.classPath.add(PathUtil.kotlinPathsForIdeaPlugin.compilerPath)

        val scriptClasspath = ScriptDependenciesManager.getInstance(environment.project).getScriptClasspath(scriptVFile)
        scriptClasspath.forEach {
            params.classPath.add(it.presentableUrl)
        }

        params.mainClass = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
        params.programParametersList.prepend(filePath)
        params.programParametersList.prepend("-script")

        return params
    }

    private fun commonParameters(): JavaParameters {
        val params = JavaParameters()
        setupJavaParameters(params)
        val jreHome = if (configuration.isAlternativeJrePathEnabled) myConfiguration.alternativeJrePath else null
        JavaParametersUtil.configureProject(environment.project, params, JavaParameters.JDK_ONLY, jreHome)
        return params
    }
}
