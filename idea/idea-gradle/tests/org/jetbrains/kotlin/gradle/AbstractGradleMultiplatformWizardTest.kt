/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.ide.projectWizard.NewProjectWizard
import com.intellij.ide.projectWizard.ProjectTypeStep
import com.intellij.ide.projectWizard.ProjectWizardTestCase
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.SimpleJavaSdkType
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.util.Couple
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.ContainerUtilRt
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.codeInsight.gradle.ExternalSystemImportingTestCase.LATEST_STABLE_GRADLE_PLUGIN_VERSION
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.jetbrains.kotlin.idea.configuration.KotlinGradleAbstractMultiplatformModuleBuilder
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.GroovyFileType
import java.io.File
import java.io.IOException

abstract class AbstractGradleMultiplatformWizardTest : ProjectWizardTestCase<AbstractProjectWizard>() {

    private val pluginVersion = LATEST_STABLE_GRADLE_PLUGIN_VERSION

    override fun createWizard(project: Project?, directory: File): AbstractProjectWizard {
        return NewProjectWizard(project, ModulesProvider.EMPTY_MODULES_PROVIDER, directory.path)
    }

    private fun Project.reconfigureGradleSettings(f: GradleProjectSettings.() -> Unit) {
        val systemSettings = ExternalSystemApiUtil.getSettings(
            this,
            externalSystemId
        )
        val projectSettings = GradleProjectSettings()
        projectSettings.f()
        val projects = ContainerUtilRt.newHashSet<Any>(systemSettings.getLinkedProjectsSettings())
        projects.remove(projectSettings)
        projects.add(projectSettings)
        systemSettings.setLinkedProjectsSettings(projects)
    }

    protected fun testImportFromBuilder(
        builder: KotlinGradleAbstractMultiplatformModuleBuilder,
        vararg testClassNames: String,
        metadataInside: Boolean = false,
        performImport: Boolean = true,
        useQualifiedModuleNames: Boolean = false
    ): Project {
        val project = createProject { step ->
            if (step is ProjectTypeStep) {
                TestCase.assertTrue(step.setSelectedTemplate("Kotlin", builder.presentableName))
                val steps = myWizard.sequence.selectedSteps
                TestCase.assertEquals(4, steps.size)
                val projectBuilder = myWizard.projectBuilder
                UsefulTestCase.assertInstanceOf(projectBuilder, builder::class.java)
                with(projectBuilder as KotlinGradleAbstractMultiplatformModuleBuilder) {
                    explicitPluginVersion = pluginVersion
                }

                myProject.reconfigureGradleSettings {
                    distributionType = DistributionType.DEFAULT_WRAPPED
                }
            }
        }

        val modules = ModuleManager.getInstance(project).modules
        TestCase.assertEquals(1, modules.size)
        val module = modules[0]
        TestCase.assertTrue(ModuleRootManager.getInstance(module).isSdkInherited)

        val root = ProjectRootManager.getInstance(project).contentRoots[0]

        val settingsScript = VfsUtilCore.findRelativeFile("settings.gradle", root)
        TestCase.assertNotNull(settingsScript)
        val settingsScriptText = StringUtil.convertLineSeparators(VfsUtilCore.loadText(settingsScript!!))
        TestCase.assertTrue("rootProject.name = " in settingsScriptText)
        if (metadataInside) {
            TestCase.assertTrue("enableFeaturePreview('GRADLE_METADATA')" in settingsScriptText)
        }

        File(root.canonicalPath).assertNoEmptyChildren()

        val buildScript = VfsUtilCore.findRelativeFile("build.gradle", root)!!
        val buildScriptText = StringUtil.convertLineSeparators(VfsUtilCore.loadText(buildScript))
        println(buildScriptText)

        if (!performImport) return project
        doImportProject(project, useQualifiedModuleNames)
        if (testClassNames.isNotEmpty()) {
            doTestProject(project, *testClassNames)
        }
        return project
    }

    private fun File.assertNoEmptyChildren() {
        for (file in walkTopDown()) {
            if (!file.isDirectory) {
                var empty = true
                file.forEachLine {
                    if (it.isNotEmpty()) {
                        empty = false
                        return@forEachLine
                    }
                }
                TestCase.assertFalse("Generated file ${file.path} is empty", empty)
            }
        }
    }

    @Throws(IOException::class)
    private fun Project.createProjectSubFile(relativePath: String): VirtualFile {
        val f = File(basePath!!, relativePath)
        FileUtil.ensureExists(f.parentFile)
        FileUtil.ensureCanCreateFile(f)
        val created = f.createNewFile()
        if (!created) {
            throw AssertionError("Unable to create the project sub file: " + f.absolutePath)
        }
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(f)!!
    }

    private fun runWrite(f: () -> Unit) {
        object : WriteAction<Any>() {
            override fun run(result: Result<Any>) {
                f()
            }
        }.execute()
    }

    private fun doImportProject(project: Project, useQualifiedModuleNames: Boolean = false) {
        ExternalSystemApiUtil.subscribe(
            project,
            GradleConstants.SYSTEM_ID,
            object : ExternalSystemSettingsListenerAdapter<ExternalProjectSettings>() {
                override fun onProjectsLinked(settings: Collection<ExternalProjectSettings>) {
                    val item = ContainerUtil.getFirstItem<Any>(settings)
                    if (item is GradleProjectSettings) {
                        item.gradleJvm = DEFAULT_SDK
                    }
                }
            })

        GradleSettings.getInstance(project).gradleVmOptions = "-Xmx256m -XX:MaxPermSize=64m"
        val wrapperJarFrom = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(GradleImportingTestCase.wrapperJar())!!
        val wrapperJarFromTo = project.createProjectSubFile("gradle/wrapper/gradle-wrapper.jar")
        runWrite {
            wrapperJarFromTo.setBinaryContent(wrapperJarFrom.contentsToByteArray())
        }

        project.reconfigureGradleSettings {
            distributionType = DistributionType.DEFAULT_WRAPPED
            externalProjectPath = project.basePath!!
            gradleJvm = DEFAULT_SDK
            isUseQualifiedModuleNames = useQualifiedModuleNames
        }

        val error = Ref.create<Couple<String>>()
        ExternalSystemUtil.refreshProjects(
            ImportSpecBuilder(project, externalSystemId)
                .use(ProgressExecutionMode.MODAL_SYNC)
                .callback(object : ExternalProjectRefreshCallback {
                    override fun onSuccess(externalProject: DataNode<ProjectData>?) {
                        if (externalProject == null) {
                            val errorMessage = "Got null External project after import"
                            System.err.println(errorMessage)
                            error.set(Couple.of(errorMessage, null))
                            return
                        }
                        ServiceManager.getService(ProjectDataManager::class.java).importData(externalProject, project, true)
                        println("External project was successfully imported")
                    }

                    override fun onFailure(errorMessage: String, errorDetails: String?) {
                        error.set(Couple.of(errorMessage, errorDetails))
                    }
                })
                .forceWhenUptodate()
        )

        if (!error.isNull) {
            var failureMsg = "Import failed: " + error.get().first
            if (StringUtil.isNotEmpty(error.get().second)) {
                failureMsg += "\nError details: \n" + error.get().second
            }
            TestCase.fail(failureMsg)
        }
    }

    private fun doTestProject(project: Project, vararg testClassNames: String) {
        val settings = GradleExecutionSettings(null, null, DistributionType.DEFAULT_WRAPPED, false)
        println("Running project tests: ${testClassNames.toList()}")
        GradleExecutionHelper().execute(project.basePath!!, settings) {
            // TODO: --no-daemon should be here, unfortunately it does not work for TestLauncher
            val testLauncher = it.newTestLauncher()
            testLauncher.withJvmTestClasses(*testClassNames).run()
        }
    }

    protected fun runTaskInProject(project: Project, taskName: String) {
        val settings = GradleExecutionSettings(null, null, DistributionType.DEFAULT_WRAPPED, false)
        println("Running project task: $taskName")
        GradleExecutionHelper().execute(project.basePath!!, settings) {
            it.newBuild().forTasks(taskName).run()
        }
    }

    override fun setUp() {
        super.setUp()
        val javaHome = IdeaTestUtil.requireRealJdkHome()
        ApplicationManager.getApplication().runWriteAction {
            addSdk(SimpleJavaSdkType().createJdk(DEFAULT_SDK, javaHome))
            addSdk(SimpleJavaSdkType().createJdk("_other", javaHome))

            println("ProjectWizardTestCase.configureJdk:")
            println(listOf(*ProjectJdkTable.getInstance().allJdks))

            FileTypeManager.getInstance().associateExtension(GroovyFileType.GROOVY_FILE_TYPE, "gradle")
        }
    }

    companion object {
        val externalSystemId = GradleConstants.SYSTEM_ID
    }
}