/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.gradle.execution

import com.intellij.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil
import com.intellij.compiler.options.CompileStepBeforeRun
import com.intellij.execution.CantRunException
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.ExecutionErrorDialog
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.execution.util.ProgramParametersUtil
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaModule
import com.intellij.task.ExecuteRunConfigurationTask
import gnu.trove.THashMap
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.plugins.gradle.execution.GradleRunnerUtil
import org.jetbrains.plugins.gradle.execution.build.GradleExecutionEnvironmentProvider
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * This provider is responsible for building [ExecutionEnvironment] for Kotlin JVM modules to be run using Gradle.
 */
class KotlinGradleAppEnvProvider : GradleExecutionEnvironmentProvider {

    override fun isApplicable(task: ExecuteRunConfigurationTask): Boolean {
        val enabled = Registry.`is`("kotlin.gradle-run.enabled", false)
        return enabled && task.runProfile is KotlinRunConfiguration
    }

    override fun createExecutionEnvironment(
        project: Project, executeRunConfigurationTask: ExecuteRunConfigurationTask, executor: Executor?
    ): ExecutionEnvironment? {

        if (!isApplicable(executeRunConfigurationTask)) return null

        val applicationConfiguration = executeRunConfigurationTask.runProfile as KotlinRunConfiguration

        val mainClass = applicationConfiguration.configurationModule?.findClass(applicationConfiguration.MAIN_CLASS_NAME) ?: return null

        val virtualFile = mainClass.containingFile.virtualFile
        val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(virtualFile) ?: return null

        val params = JavaParameters().apply {
            JavaParametersUtil.configureConfiguration(this, applicationConfiguration)
            this.vmParametersList.addParametersString(applicationConfiguration.vmParameters)
        }

        val javaModuleName: String?
        val javaExePath: String
        try {
            val jdk = JavaParametersUtil.createProjectJdk(project, applicationConfiguration.alternativeJrePath)
                ?: throw RuntimeException(ExecutionBundle.message("run.configuration.error.no.jdk.specified"))
            val type = jdk.sdkType
            if (type !is JavaSdkType) throw RuntimeException(ExecutionBundle.message("run.configuration.error.no.jdk.specified"))
            javaExePath = (type as JavaSdkType).getVMExecutablePath(jdk)?.let {
                FileUtil.toSystemIndependentName(it)
            } ?: throw RuntimeException(ExecutionBundle.message("run.configuration.cannot.find.vm.executable"))
            javaModuleName = findJavaModuleName(jdk, applicationConfiguration.configurationModule, mainClass)
        } catch (e: CantRunException) {
            ExecutionErrorDialog.show(e, "Cannot use specified JRE", project)
            throw RuntimeException(ExecutionBundle.message("run.configuration.cannot.find.vm.executable"))
        }

        val className = mainClass.name ?: return null
        val runAppTaskName = "$className.main()"

        val taskSettings = ExternalSystemTaskExecutionSettings().apply {
            isPassParentEnvs = params.isPassParentEnvs
            env = if (params.env.isEmpty()) emptyMap() else THashMap(params.env)
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            externalProjectPath = GradleRunnerUtil.resolveProjectPath(module)
            taskNames = listOf(runAppTaskName)
        }

        val executorId = executor?.id ?: DefaultRunExecutor.EXECUTOR_ID
        val environment = ExternalSystemUtil.createExecutionEnvironment(project, GradleConstants.SYSTEM_ID, taskSettings, executorId)
            ?: return null
        val runnerAndConfigurationSettings = environment.runnerAndConfigurationSettings ?: return null
        val gradleRunConfiguration = runnerAndConfigurationSettings.configuration as ExternalSystemRunConfiguration

        val sourceSetName = when {
            GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY == ExternalSystemApiUtil.getExternalModuleType(
                module
            ) -> GradleProjectResolverUtil.getSourceSetName(module)
            ModuleRootManager.getInstance(module).fileIndex.isInTestSourceContent(virtualFile) -> "test"
            else -> "main"
        } ?: return null

        val initScript = generateInitScript(
            applicationConfiguration, project, module, params, runAppTaskName,
            mainClass, javaExePath, sourceSetName, javaModuleName
        )
        gradleRunConfiguration.putUserData<String>(GradleTaskManager.INIT_SCRIPT_KEY, initScript)
        gradleRunConfiguration.putUserData<String>(GradleTaskManager.INIT_SCRIPT_PREFIX_KEY, runAppTaskName)

        // reuse all before tasks except 'Make' as it doesn't make sense for delegated run
        gradleRunConfiguration.beforeRunTasks = RunManagerImpl.getInstanceImpl(project).getBeforeRunTasks(applicationConfiguration)
            .filter { it.providerId !== CompileStepBeforeRun.ID }
        return environment
    }

    companion object {
        private fun createEscapedParameters(parameters: List<String>, prefix: String): String {
            val result = StringBuilder()
            for (parameter in parameters) {
                if (StringUtil.isEmpty(parameter)) continue
                val escaped = StringUtil.escapeChars(parameter, '\\', '"', '\'')
                result.append(prefix).append(" '").append(escaped).append("'\n")
            }
            return result.toString()
        }

        private fun findJavaModuleName(sdk: Sdk, module: JavaRunConfigurationModule, mainClass: PsiClass): String? {
            return if (JavaSdkUtil.isJdkAtLeast(sdk, JavaSdkVersion.JDK_1_9)) {
                DumbService.getInstance(module.project).computeWithAlternativeResolveEnabled<PsiJavaModule, RuntimeException> {
                    JavaModuleGraphUtil.findDescriptorByElement(module.findClass(mainClass.qualifiedName))
                }?.name ?: return null
            } else null
        }

        private fun generateInitScript(
            applicationConfiguration: KotlinRunConfiguration, project: Project, module: Module,
            params: JavaParameters, runAppTaskName: String, mainClass: PsiClass, javaExePath: String,
            sourceSetName: String, javaModuleName: String?
        ): String? {
            // Init script creates the run task only for the project matching 'applicationConfiguration'.
            // To find the proper one we compare identifiers: the one provided by Gradle and the one we generate based on our project import
            // data (external module data).

            val extProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
            val extProjectInfo = ExternalSystemUtil.getExternalProjectInfo(project, GradleConstants.SYSTEM_ID, extProjectPath) ?: return null
            val extModuleData = GradleProjectResolverUtil.findModule(extProjectInfo.externalProjectStructure, extProjectPath) ?: return null

            // Pair of 'project.rootProject.name' and 'project.path' is a unique project id in Gradle (including composite builds).
            // So our one is built using the same principle.
            val projectPath = extModuleData.data.id
            val gradleProjectId = if (projectPath.startsWith(':')) { // shortening (is unique project id)
                val rootProjectName = (extModuleData.parent?.data as? ProjectData)?.externalName ?: ""
                rootProjectName + projectPath
            } else {
                projectPath.takeIf { ':' in it } ?: "$projectPath:" // includes rootProject.name already, for top level projects has no ':'
            }

            val workingDir = ProgramParametersUtil.getWorkingDir(applicationConfiguration, project, module)?.let {
                FileUtil.toSystemIndependentName(it)
            }

            val argsString = createEscapedParameters(params.programParametersList.parameters, "args") +
                    createEscapedParameters(params.vmParametersList.parameters, "jvmArgs")

            // @formatter:off
            @Suppress("UnnecessaryVariable")
//      @Language("Groovy")
            val initScript = """
    def gradleProjectId = '$gradleProjectId'
    def runAppTaskName = '$runAppTaskName'
    def mainClass = '${mainClass.qualifiedName}'
    def javaExePath = '$javaExePath'
    def _workingDir = ${if (workingDir.isNullOrEmpty()) "null\n" else "'$workingDir'\n"}
    def sourceSetName = '$sourceSetName'
    def javaModuleName = ${if (javaModuleName == null) "null\n" else "'$javaModuleName'\n"}

    allprojects {
        afterEvaluate { project ->
            if (project.rootProject.name + project.path == gradleProjectId) {
                def overwrite = project.tasks.findByName(runAppTaskName) != null
                project.tasks.create(name: runAppTaskName, overwrite: overwrite, type: JavaExec) {
                    if (javaExePath) executable = javaExePath
                    if (project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                        project.kotlin.targets.each { target ->
                            target.compilations.each { compilation ->
                                if (compilation.defaultSourceSetName == sourceSetName) {
                                    classpath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
                                }
                            }
                        }
                    } else {
                        classpath = project.sourceSets[sourceSetName].runtimeClasspath
                    }
    
                    main = mainClass
                    $argsString
                    if(_workingDir) workingDir = _workingDir
                    standardInput = System.in
                    if(javaModuleName) {
                        inputs.property('moduleName', javaModuleName)
                        doFirst {
                            jvmArgs += [
                                    '--module-path', classpath.asPath,
                                    '--module', javaModuleName + '/' + mainClass
                            ]
                            classpath = files()
                        }
                    }
                }
            }
        }
    }
    """
            // @formatter:on
            return initScript
        }
    }
}