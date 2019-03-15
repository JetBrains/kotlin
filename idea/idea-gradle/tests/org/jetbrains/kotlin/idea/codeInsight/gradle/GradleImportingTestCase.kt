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
package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.compiler.server.BuildManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import junit.framework.TestCase
import org.gradle.util.GradleVersion
import org.gradle.wrapper.GradleWrapperMain
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.test.KotlinSdkCreationChecker
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.GroovyFileType
import org.junit.Assume.assumeThat
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.net.URISyntaxException
import java.util.*
import org.junit.AfterClass
import org.junit.Assume.assumeTrue

// part of org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
@RunWith(value = Parameterized::class)
abstract class GradleImportingTestCase : ExternalSystemImportingTestCase() {

    protected var sdkCreationChecker : KotlinSdkCreationChecker? = null

    @JvmField
    @Rule
    var name = TestName()

    @JvmField
    @Rule
    var testWatcher = ImportingTestWatcher()

    @JvmField
    @Rule
    var versionMatcherRule = VersionMatcherRule()

    @JvmField
    @Parameterized.Parameter
    var gradleVersion: String = ""

    private lateinit var myProjectSettings: GradleProjectSettings
    private lateinit var myJdkHome: String

    open fun isApplicableTest(): Boolean = true

    override fun setUp() {
        myJdkHome = IdeaTestUtil.requireRealJdkHome()
        super.setUp()
        assumeTrue(isApplicableTest())
        assumeThat(gradleVersion, versionMatcherRule.matcher)
        runWrite {
            ProjectJdkTable.getInstance().findJdk(GRADLE_JDK_NAME)?.let {
                ProjectJdkTable.getInstance().removeJdk(it)
            }
            val jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(myJdkHome))!!
            val jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), jdkHomeDir, JavaSdk.getInstance(), true, null, GRADLE_JDK_NAME)
            TestCase.assertNotNull("Cannot create JDK for $myJdkHome", jdk)
            ProjectJdkTable.getInstance().addJdk(jdk!!)
            FileTypeManager.getInstance().associateExtension(GroovyFileType.GROOVY_FILE_TYPE, "gradle")

        }
        myProjectSettings = GradleProjectSettings().apply {
            this.isUseQualifiedModuleNames = false
        }
        GradleSettings.getInstance(myProject).gradleVmOptions = "-Xmx128m -XX:MaxPermSize=64m"
        System.setProperty(ExternalSystemExecutionSettings.REMOTE_PROCESS_IDLE_TTL_IN_MS_KEY, GRADLE_DAEMON_TTL_MS.toString())
        configureWrapper()
        sdkCreationChecker = KotlinSdkCreationChecker()
    }

    override fun tearDown() {
        try {
            runWrite {
                val old = ProjectJdkTable.getInstance().findJdk(GRADLE_JDK_NAME)
                if (old != null) {
                    SdkConfigurationUtil.removeSdk(old)
                }
            }

            Messages.setTestDialog(TestDialog.DEFAULT)
            FileUtil.delete(BuildManager.getInstance().buildSystemDirectory.toFile())
            sdkCreationChecker?.removeNewKotlinSdk()
        } finally {
            super.tearDown()
        }
    }

    override fun collectAllowedRoots(roots: MutableList<String>) {
        roots.add(myJdkHome)
        roots.addAll(ExternalSystemTestCase.collectRootsInside(myJdkHome))
        roots.add(PathManager.getConfigPath())
    }

    override fun getName(): String {
        return if (name.methodName == null) super.getName() else FileUtil.sanitizeFileName(name.methodName)
    }

    override fun getTestsTempDir(): String = "gradleImportTests"

    override fun getExternalSystemConfigFileName(): String = "build.gradle"

    protected fun importProjectUsingSingeModulePerGradleProject() {
        myProjectSettings.isResolveModulePerSourceSet = false
        importProject()
    }

    override fun importProject() {
        ExternalSystemApiUtil.subscribe(
            myProject,
            GradleConstants.SYSTEM_ID,
            object : ExternalSystemSettingsListenerAdapter<ExternalProjectSettings>() {
                override fun onProjectsLinked(settings: Collection<ExternalProjectSettings>) {
                    val item = ContainerUtil.getFirstItem<Any>(settings)
                    if (item is GradleProjectSettings) {
                        item.gradleJvm = GRADLE_JDK_NAME
                    }
                }
            })
        super.importProject()
    }

    override fun importProject(@NonNls @Language("Groovy") config: String) {
        super.importProject(
            """
                allprojects {
                    repositories {
                        maven {
                            url 'https://maven.labs.intellij.net/repo1'
                        }
                    }
                }

                $config
                """.trimIndent()
        )
    }

    override fun getCurrentExternalProjectSettings(): GradleProjectSettings = myProjectSettings

    override fun getExternalSystemId(): ProjectSystemId = GradleConstants.SYSTEM_ID

    @Throws(IOException::class, URISyntaxException::class)
    private fun configureWrapper() {
        val distributionUri = AbstractModelBuilderTest.DistributionLocator().getDistributionFor(GradleVersion.version(gradleVersion))

        myProjectSettings.distributionType = DistributionType.DEFAULT_WRAPPED
        val wrapperJarFrom = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(wrapperJar())!!

        val wrapperJarFromTo = createProjectSubFile("gradle/wrapper/gradle-wrapper.jar")
        runWrite {
            wrapperJarFromTo.setBinaryContent(wrapperJarFrom.contentsToByteArray())
        }

        val properties = Properties()
        properties.setProperty("distributionBase", "GRADLE_USER_HOME")
        properties.setProperty("distributionPath", "wrapper/dists")
        properties.setProperty("zipStoreBase", "GRADLE_USER_HOME")
        properties.setProperty("zipStorePath", "wrapper/dists")
        properties.setProperty("distributionUrl", distributionUri.toString())

        val writer = StringWriter()
        properties.store(writer, null)

        createProjectSubFile("gradle/wrapper/gradle-wrapper.properties", writer.toString())
    }

    protected open fun testDataDirName(): String = ""

    protected fun testDataDirectory(): File {
        val baseDir = "${PluginTestCaseBase.getTestDataPathBase()}/gradle/${testDataDirName()}/"
        return File(baseDir, getTestName(true).substringBefore("_"))
    }

    protected open fun configureByFiles(): List<VirtualFile> {
        val rootDir = testDataDirectory()
        assert(rootDir.exists()) { "Directory ${rootDir.path} doesn't exist" }

        return rootDir.walk().mapNotNull {
            when {
                it.isDirectory -> null
                !it.name.endsWith(SUFFIX) -> {
                    createProjectSubFile(it.path.substringAfter(rootDir.path + File.separator), it.readText())
                }
                else -> null
            }
        }.toList()
    }

    protected fun importProjectFromTestData(): List<VirtualFile> {
        val files = configureByFiles()
        importProject()
        return files
    }

    protected fun checkFiles(files: List<VirtualFile>) {
        FileDocumentManager.getInstance().saveAllDocuments()

        files.filter {
            it.name == GradleConstants.DEFAULT_SCRIPT_NAME
                    || it.name == GradleConstants.KOTLIN_DSL_SCRIPT_NAME
                    || it.name == GradleConstants.SETTINGS_FILE_NAME
        }
            .forEach {
                if (it.name == GradleConstants.SETTINGS_FILE_NAME && !File(testDataDirectory(), it.name + SUFFIX).exists()) return@forEach
                val actualText = LoadTextUtil.loadText(it).toString()
                val expectedFileName = if (File(testDataDirectory(), it.name + ".$gradleVersion" + SUFFIX).exists()) {
                    it.name + ".$gradleVersion" + SUFFIX
                } else {
                    it.name + SUFFIX
                }
                KotlinTestUtils.assertEqualsToFile(File(testDataDirectory(), expectedFileName), actualText)
            }
    }


    private fun runWrite(f: () -> Unit) {
        object : WriteAction<Any>() {
            override fun run(result: Result<Any>) {
                f()
            }
        }.execute()
    }

    companion object {
        const val GRADLE_JDK_NAME = "Gradle JDK"
        private const val GRADLE_DAEMON_TTL_MS = 10000

        @JvmStatic
        protected val SUFFIX = ".after"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: with Gradle-{0}")
        fun data(): Collection<Array<Any>> {
            return Arrays.asList(*AbstractModelBuilderTest.SUPPORTED_GRADLE_VERSIONS)
        }

        fun wrapperJar(): File {
            return File(PathUtil.getJarPathForClass(GradleWrapperMain::class.java))
        }

        private var logSaver: GradleImportingTestLogSaver? = null

        @JvmStatic
        @BeforeClass
        fun setLoggerFactory() {
            logSaver = GradleImportingTestLogSaver()
        }

        @JvmStatic
        @AfterClass
        fun restoreLoggerFactory() {
            logSaver?.restore()
        }
    }
}
