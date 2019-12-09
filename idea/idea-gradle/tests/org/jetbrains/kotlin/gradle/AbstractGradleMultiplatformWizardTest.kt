/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import assertk.Assert
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
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
import org.gradle.api.tasks.testing.TestExecutionException
import org.jetbrains.kotlin.idea.codeInsight.gradle.ExternalSystemImportingTestCase.LATEST_STABLE_GRADLE_PLUGIN_VERSION
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.jetbrains.kotlin.idea.configuration.KotlinGradleAbstractMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.TargetSupportException
import org.jetbrains.kotlin.test.isIgnoredInDatabaseWithLog
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME
import org.jetbrains.plugins.groovy.GroovyFileType
import java.io.File
import java.io.FileNotFoundException
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

    fun KotlinGradleAbstractMultiplatformModuleBuilder.buildProject(): Project =
        createProject { step ->
            if (step is ProjectTypeStep) {
                TestCase.assertTrue(step.setSelectedTemplate("Kotlin", this.presentableName))
                val steps = myWizard.sequence.selectedSteps
                TestCase.assertEquals(4, steps.size)
                val projectBuilder = myWizard.projectBuilder
                UsefulTestCase.assertInstanceOf(projectBuilder, this::class.java)
                with(projectBuilder as KotlinGradleAbstractMultiplatformModuleBuilder) {
                    explicitPluginVersion = pluginVersion
                }

                myProject.reconfigureGradleSettings {
                    distributionType = DistributionType.DEFAULT_WRAPPED
                }
            }
        }

    fun Project.checkGradleConfiguration(metadataInside: Boolean = false, mppPluginInside: Boolean = true) {

        val modules = ModuleManager.getInstance(this).modules
        assertThat(modules, "modules in project").size().isEqualTo(1)

        val module = modules[0]
        assertThat(ModuleRootManager.getInstance(module).isSdkInherited, "sdk inherited").isTrue()

        val root = ProjectRootManager.getInstance(this).contentRoots[0]

        val settingsScript = VfsUtilCore.findRelativeFile(SETTINGS_FILE_NAME, root)
        assertThat(settingsScript, SETTINGS_FILE_NAME).isNotNull()

        TestCase.assertNotNull(settingsScript)
        val settingsScriptText = StringUtil.convertLineSeparators(VfsUtilCore.loadText(settingsScript!!))

        assertThat(settingsScriptText, SETTINGS_FILE_NAME).contains("rootProject.name = ")

        if (metadataInside) {
            assertThat(settingsScriptText, SETTINGS_FILE_NAME).contains("enableFeaturePreview('GRADLE_METADATA')")
        }

        File(root.canonicalPath).assertNoEmptyChildren()

        val buildScript = VfsUtilCore.findRelativeFile(DEFAULT_SCRIPT_NAME, root)!!
        val buildScriptText = StringUtil.convertLineSeparators(VfsUtilCore.loadText(buildScript))

        if (mppPluginInside) {
            assertThat(buildScriptText, DEFAULT_SCRIPT_NAME).contains("id 'org.jetbrains.kotlin.multiplatform' version '$pluginVersion'")
        }
        println(buildScriptText)
    }

    protected fun Project.runGradleImport(useQualifiedModuleNames: Boolean = false) {
        ExternalSystemApiUtil.subscribe(
            this,
            GradleConstants.SYSTEM_ID,
            object : ExternalSystemSettingsListenerAdapter<ExternalProjectSettings>() {
                override fun onProjectsLinked(settings: Collection<ExternalProjectSettings>) {
                    val item = ContainerUtil.getFirstItem<Any>(settings)
                    if (item is GradleProjectSettings) {
                        item.gradleJvm = DEFAULT_SDK
                    }
                }
            })

        GradleSettings.getInstance(this).gradleVmOptions = "-Xmx256m -XX:MaxPermSize=64m"
        val wrapperJarFrom = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(GradleImportingTestCase.wrapperJar())!!
        val wrapperJarFromTo = this.createProjectSubFile("gradle/wrapper/gradle-wrapper.jar")
        runWrite {
            wrapperJarFromTo.setBinaryContent(wrapperJarFrom.contentsToByteArray())
        }

        this.reconfigureGradleSettings {
            distributionType = DistributionType.DEFAULT_WRAPPED
            externalProjectPath = this@runGradleImport.basePath!!
            gradleJvm = DEFAULT_SDK
            isUseQualifiedModuleNames = useQualifiedModuleNames
        }

        val error = Ref.create<Couple<String>>()
        ExternalSystemUtil.refreshProjects(
            ImportSpecBuilder(this, externalSystemId)
                .use(ProgressExecutionMode.MODAL_SYNC)
                .callback(object : ExternalProjectRefreshCallback {
                    override fun onSuccess(externalProject: DataNode<ProjectData>?) {
                        if (externalProject == null) {
                            val errorMessage = "Got null External project after import"
                            System.err.println(errorMessage)
                            error.set(Couple.of(errorMessage, null))
                            return
                        }
                        ServiceManager.getService(ProjectDataManager::class.java).importData(externalProject, this@runGradleImport, true)
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

    protected fun Project.runGradleTests(vararg testClassNames: String) {
        val settings = GradleExecutionSettings(null, null, DistributionType.DEFAULT_WRAPPED, false)
        println("Running project tests: ${testClassNames.toList()}")

        val errors = mutableMapOf<String, Throwable>()
        testClassNames.forEach { test ->
            try {
                GradleExecutionHelper().execute(this.basePath!!, settings) {
                    // TODO: --no-daemon should be here, unfortunately it does not work for TestLauncher
                    val testLauncher = it.newTestLauncher()
                    testLauncher.withJvmTestClasses(test).run()
                }
            } catch (exception: Throwable) {
                errors[test] = exception
            }
        }
        if (errors.isNotEmpty()) {
            throw TestExecutionException(
                "${errors.size} runs failed: \n" +
                        " ${errors.map { "TestClass: ${it.component1()}, ExceptionMessage: ${it.component2().localizedMessage} \n" }}"
            )
        }
    }

    protected fun Project.runGradleTask(taskName: String) {
        val settings = GradleExecutionSettings(null, null, DistributionType.DEFAULT_WRAPPED, false)
        println("Running project task: $taskName")
        GradleExecutionHelper().execute(this.basePath!!, settings) {
            it.newBuild().forTasks(taskName).run()
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

    override fun setUp() {
        super.setUp()
        val javaHome = IdeaTestUtil.requireRealJdkHome()
        ApplicationManager.getApplication().runWriteAction {
            addSdk(SimpleJavaSdkType().createJdk(DEFAULT_SDK, javaHome))
            addSdk(SimpleJavaSdkType().createJdk("_other", javaHome))

            println("ProjectWizardTestCase.configureJdk:")
            println(listOf(*getProjectJdkTableSafe().allJdks))

            FileTypeManager.getInstance().associateExtension(GroovyFileType.GROOVY_FILE_TYPE, "gradle")
        }
    }

    override fun shouldRunTest(): Boolean {
        return super.shouldRunTest() && !isIgnoredInDatabaseWithLog(this)
    }

    companion object {
        val externalSystemId = GradleConstants.SYSTEM_ID

        private val defaultNativeTarget by lazy {
            try {
                HostManager.host
            } catch (e: TargetSupportException) {
                KonanTarget.IOS_X64
            }
        }

        // Examples: ios_x64 -> ios, macos_x64 -> macos, wasm32 -> wasm.
        private val KonanTarget.userTargetName: String
            get() {
                val index = name.indexOfAny("_0123456789".toCharArray())
                return if (index > 0) name.substring(0, index) else name
            }

        val native by lazy { defaultNativeTarget.userTargetName }
    }


    class FileChecker(val project: Project) {
        val kotlin = "kotlin"
        val sample = "sample"

        val commonMain = "commonMain"
        val commonTest = "commonTest"
        val jvmMain = "jvmMain"
        val jvmTest = "jvmTest"
        val jsMain = "jsMain"
        val jsTest = "jsTest"
        val nativeMain = "${native}Main"
        val nativeTest = "${native}Test"
        val iosMain = "iosMain"
        val iosTest = "iosTest"

        private var files: MutableMap<String, (Assert<String>.() -> Unit)?> = mutableMapOf()
        private var sourceSetsCount: Int? = null
        private var commons: MutableMap<String, (Assert<String>.() -> Unit)?> = mutableMapOf()
        private var tests: MutableMap<String, (Assert<String>.() -> Unit)?> = mutableMapOf()
        private var mains: MutableMap<String, (Assert<String>.() -> Unit)?> = mutableMapOf()

        fun isExist(uri: String, matcher: (Assert<String>.() -> Unit)? = null) {
            files[uri] = matcher
        }

        fun common(uri: String, matcher: (Assert<String>.() -> Unit)? = null) {
            commons[uri] = matcher
        }

        fun test(uri: String, matcher: (Assert<String>.() -> Unit)? = null) {
            tests[uri] = matcher
        }

        fun main(uri: String, matcher: (Assert<String>.() -> Unit)? = null) {
            mains[uri] = matcher
        }

        fun sourceSetsSize(value: Int) {
            sourceSetsCount = value
        }

        fun runChecks(source: String?) {
            assertAll {
                val root = ProjectRootManager.getInstance(project).contentRoots[0]
                val src = source?.let { root.findFileByRelativePath(source) ?: throw FileNotFoundException(source) } ?: root

                sourceSetsCount?.let { count ->
                    assertThat(src.children.filter { it.isDirectory }, "sourceSet folders").size().isEqualTo(count)
                }
                files.forEach { (uri, matcher) ->
                    val content = src.getIfExists(uri).getContent()

                    matcher?.let {
                        assertThat(content, uri).it()
                    }
                }
                tests.forEach { (uri, matcher) ->
                    val content = src.getIfExists(uri).getContent()

                    assertThat(content, uri).contains("@Test")
                    matcher?.let {
                        assertThat(content, uri).it()
                    }
                }
                mains.forEach { (uri, matcher) ->
                    val content = src.getIfExists(uri).getContent()

                    assertThat(content, uri).contains("actual")
                    matcher?.let {
                        assertThat(content, uri).it()
                    }
                }
                commons.forEach { (uri, matcher) ->
                    val content = src.getIfExists(uri).getContent()

                    assertThat(content, uri).contains("expect")
                    matcher?.let {
                        assertThat(content, uri).it()
                    }
                }
            }
        }
    }

    fun Project.checkSource(source: String? = null, addChecks: FileChecker.() -> Unit) {
        val checker = FileChecker(this)
        checker.addChecks()
        checker.runChecks(source)
    }
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

private fun VirtualFile.getContent(): String = StringUtil.convertLineSeparators(VfsUtilCore.loadText(this))

private fun VirtualFile.getIfExists(
    uri: String,
    isNotEmpty: Boolean = true
): VirtualFile {
    val file = this.findFileByRelativePath(uri)
    assertThat(file, uri).isNotNull()

    if (isNotEmpty) {
        File(file!!.canonicalPath).assertNoEmptyChildren()
    }
    return file!!
}