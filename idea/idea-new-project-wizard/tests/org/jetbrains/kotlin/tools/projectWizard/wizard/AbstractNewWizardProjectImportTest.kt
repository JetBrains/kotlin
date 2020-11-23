/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.SimpleJavaSdkType
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.PlatformTestCase
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.codeInsight.gradle.ExternalSystemImportingTestCase
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.utils.getKtFile
import org.jetbrains.kotlin.idea.scripting.gradle.getGradleProjectSettings
import org.jetbrains.kotlin.idea.test.KotlinSdkCreationChecker
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.jetbrains.kotlin.tools.projectWizard.cli.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.Services
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.wizard.service.IdeaServices
import org.jetbrains.kotlin.tools.projectWizard.wizard.service.IdeaWizardService
import org.jetbrains.kotlin.tools.projectWizard.wizard.services.TestWizardServices
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleEnvironment
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

//TODO change to HeavyPlatformTestCase when we stop supporting <= 192
abstract class AbstractNewWizardProjectImportTest : PlatformTestCase() {
    abstract fun createWizard(directory: Path, buildSystem: BuildSystem, projectDirectory: Path): Wizard

    lateinit var sdkCreationChecker: KotlinSdkCreationChecker

    override fun setUp() {
        super.setUp()
        runWriteAction {
            PluginTestCaseBase.addJdk(testRootDisposable) {
                JavaSdk.getInstance().createJdk(SDK_NAME, IdeaTestUtil.requireRealJdkHome(), false)
            }
        }
        sdkCreationChecker = KotlinSdkCreationChecker()
    }

    override fun tearDown() {
        sdkCreationChecker.removeNewKotlinSdk()
        super.tearDown()
        runWriteAction {
            ProjectJdkTable.getInstance().findJdk(SDK_NAME)?.let(ProjectJdkTable.getInstance()::removeJdk)
        }
    }

    fun doTestGradleKts(directoryPath: String) {
        doTest(directoryPath, BuildSystem.GRADLE_KOTLIN_DSL)
        checkScriptConfigurationsIfAny()
    }

    fun doTestGradleGroovy(directoryPath: String) {
        doTest(directoryPath, BuildSystem.GRADLE_GROOVY_DSL)
    }

    fun doTestMaven(directoryPath: String) {
        doTest(directoryPath, BuildSystem.MAVEN)
    }

    private fun doTest(directoryPath: String, buildSystem: BuildSystem) {
        val directory = Paths.get(directoryPath)

        val parameters = DefaultTestParameters.fromTestDataOrDefault(directory)
        if (!parameters.runForMaven && buildSystem == BuildSystem.MAVEN) return
        if (!parameters.runForGradleGroovy && buildSystem == BuildSystem.GRADLE_GROOVY_DSL) return

        val tempDirectory = Files.createTempDirectory(null)
        if (buildSystem.isGradle) {
            prepareGradleBuildSystem(tempDirectory)
        }

        runWizard(directory, buildSystem, tempDirectory)
    }

    protected fun runWizard(
        directory: Path,
        buildSystem: BuildSystem,
        tempDirectory: Path
    ) {
        val wizard = createWizard(directory, buildSystem, tempDirectory)

        val projectDependentServices =
            IdeaServices.createScopeDependent(project) +
                    TestWizardServices.createProjectDependent(project) +
                    TestWizardServices.PROJECT_INDEPENDENT
        wizard.apply(projectDependentServices, GenerationPhase.ALL).assertSuccess()
    }

    protected fun prepareGradleBuildSystem(
        directory: Path,
        distributionTypeSettings: DistributionType = DistributionType.WRAPPED
    ) {
        com.intellij.openapi.components.ServiceManager.getService(project, GradleSettings::class.java)?.apply {
            isOfflineWork = GradleEnvironment.Headless.GRADLE_OFFLINE?.toBoolean() ?: isOfflineWork
            serviceDirectoryPath = GradleEnvironment.Headless.GRADLE_SERVICE_DIRECTORY ?: serviceDirectoryPath
        }

        // not needed on 192 (and causes error on 192 ):
        if (!is192()) {
            val settings = GradleProjectSettings().apply {
                externalProjectPath = directory.toString()
                isUseAutoImport = false
                isUseQualifiedModuleNames = true
                gradleJvm = SDK_NAME
                distributionType = distributionTypeSettings
            }
            ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).linkProject(settings)
        }
    }

    protected fun checkScriptConfigurationsIfAny() {
        if (is192()) return

        val settings = getGradleProjectSettings(project).firstOrNull() ?: error("Cannot find linked gradle project: ${project.basePath}")
        val scripts = File(settings.externalProjectPath).walkTopDown().filter {
            it.name.endsWith("gradle.kts")
        }
        scripts.forEach {
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(it)!!
            val psiFile = project.getKtFile(virtualFile) ?: error("Cannot find KtFile for $it")
            assertTrue(
                "Configuration for ${it.path} is missing",
                project.service<ScriptConfigurationManager>().hasConfiguration(psiFile)
            )

            val bindingContext = psiFile.analyzeWithContent()

            val diagnostics = bindingContext.diagnostics.filter { it.severity == Severity.ERROR }
            assert(diagnostics.isEmpty()) {
                "Diagnostics list should be empty:\n ${diagnostics.joinToString("\n") { DefaultErrorMessages.render(it) }}"
            }
        }
    }

    private fun is192() =
        ApplicationInfoImpl.getShadowInstance().minorVersionMainPart == "2"
                && ApplicationInfoImpl.getShadowInstance().majorVersion == "2019"

    companion object {
        private const val SDK_NAME = "defaultSdk"

        val IDE_WIZARD_TEST_SERVICES_MANAGER = ServicesManager(
            IdeaServices.PROJECT_INDEPENDENT + Services.IDEA_INDEPENDENT_SERVICES
        ) { services ->
            services.firstIsInstanceOrNull<TestWizardService>()
                ?: services.firstIsInstanceOrNull<IdeaWizardService>()
                ?: services.firstOrNull()
        }
    }
}