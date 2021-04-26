/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * This TestCase implements possibility to test import with different versions of gradle and different
 * versions of gradle kotlin plugin
 */
package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.importing.ImportSpec
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkTableImpl
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.VfsTestUtil
import com.intellij.util.ArrayUtilRt
import com.intellij.util.PathUtil
import com.intellij.util.SmartList
import com.intellij.util.ThrowableRunnable
import com.intellij.util.containers.ContainerUtil
import junit.framework.TestCase
import org.gradle.StartParameter
import org.gradle.util.GradleVersion
import org.gradle.wrapper.GradleWrapperMain
import org.gradle.wrapper.PathAssembler
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.test.GradleProcessOutputInterceptor
import org.jetbrains.kotlin.idea.test.KotlinSdkCreationChecker
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.test.JUnitParameterizedWithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.RunnerFactoryWithMuteInDatabase
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.settings.GradleSystemSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil
import org.jetbrains.plugins.groovy.GroovyFileType
import org.junit.Assume
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.net.URISyntaxException
import java.util.*
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.collections.HashMap

@RunWith(value = JUnitParameterizedWithIdeaConfigurationRunner::class)
@Parameterized.UseParametersRunnerFactory(RunnerFactoryWithMuteInDatabase::class)
abstract class MultiplePluginVersionGradleImportingTestCase : ExternalSystemImportingTestCase() {
    protected var sdkCreationChecker: KotlinSdkCreationChecker? = null

    private val removedSdks: MutableList<Sdk> = SmartList()

    @JvmField
    @Rule
    var name = TestName()

    @JvmField
    @Rule
    var testWatcher = ImportingTestWatcher()

    @JvmField
    @Parameterized.Parameter(0)
    var gradleVersion: String = ""

    @JvmField
    @Parameterized.Parameter(1)
    var gradleKotlinPluginParameter: String = ""

    open val gradleKotlinPluginVersion: String get() = if (gradleKotlinPluginParameter == "master") MASTER_VERSION_OF_PLUGIN else gradleKotlinPluginParameter

    @Rule
    @JvmField
    var pluginVersionMatchingRule = PluginTargetVersionsRule()

    val project: Project
        get() = myProject

    private val isMasterVersion: Boolean
        get() = gradleKotlinPluginVersion == MASTER_VERSION_OF_PLUGIN

    open fun isApplicableTest(): Boolean {
        return pluginVersionMatchingRule.matches(
            gradleVersion,
            gradleKotlinPluginVersion.substringBefore("-"),
            isMasterVersion
        )
    }

    private lateinit var myProjectSettings: GradleProjectSettings
    private lateinit var myJdkHome: String

    open fun jvmHeapArgsByGradleVersion(version: String): String = when {
        version.startsWith("4.") ->
            // work-around due to memory leak in class loaders in gradle. The amount of used memory in the gradle daemon
            // is drammatically increased on every reimport of project due to sequential compilation of build scripts.
            // see more details in https://github.com/gradle/gradle/commit/b483d29f315758913791fe58d572fa6bafa0395c
            "-Xmx256m -XX:MaxPermSize=64m"
        else ->
            // 128M should be enough for gradle 5.0+ (leak is fixed), and <4.0 (amount of tests is less)
            "-Xms128M -Xmx256m -XX:MaxPermSize=64m"
    }

    override fun setUp() {
        myJdkHome = IdeaTestUtil.requireRealJdkHome()
        super.setUp()
        Assume.assumeTrue(isApplicableTest())
        removedSdks.clear()
        runWrite {
            val jdkTable = getProjectJdkTableSafe()
            jdkTable.findJdk(GRADLE_JDK_NAME)?.let { jdkTable.removeJdk(it) }
            val jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(myJdkHome))!!
            val jdk = SdkConfigurationUtil.setupSdk(
                arrayOfNulls(0), jdkHomeDir, JavaSdk.getInstance(), true, null,
                GRADLE_JDK_NAME
            )
            TestCase.assertNotNull("Cannot create JDK for $myJdkHome", jdk)
            if (!jdkTable.allJdks.contains(jdk)) {
                (jdkTable as ProjectJdkTableImpl).addTestJdk(jdk!!, testRootDisposable)
                ProjectRootManager.getInstance(myProject).projectSdk = jdk
            }
            FileTypeManager.getInstance().associateExtension(GroovyFileType.GROOVY_FILE_TYPE, "gradle")
        }
        myProjectSettings = GradleProjectSettings().apply {
            this.isUseQualifiedModuleNames = false
        }
        System.setProperty(
            ExternalSystemExecutionSettings.REMOTE_PROCESS_IDLE_TTL_IN_MS_KEY,
            GRADLE_DAEMON_TTL_MS.toString()
        )

        val distribution = WriteAction.computeAndWait<PathAssembler.LocalDistribution, Throwable> { configureWrapper() }

        val allowedRoots = ArrayList<String>()
        collectAllowedRoots(allowedRoots, distribution)
        if (allowedRoots.isNotEmpty()) {
            VfsRootAccess.allowRootAccess(myTestFixture.testRootDisposable, *ArrayUtilRt.toStringArray(allowedRoots))
        }

        GradleSettings.getInstance(myProject).gradleVmOptions =
            "${jvmHeapArgsByGradleVersion(gradleVersion)} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${System.getProperty("user.dir")}"

        sdkCreationChecker = KotlinSdkCreationChecker()

        GradleProcessOutputInterceptor.install(testRootDisposable)
    }

    override fun tearDown() {
        RunAll(
            ThrowableRunnable {
                runWrite {
                    Arrays.stream(ProjectJdkTable.getInstance().allJdks).forEach { jdk: Sdk ->
                        (ProjectJdkTable.getInstance() as ProjectJdkTableImpl).removeTestJdk(jdk)
                        if (jdk is Disposable) {
                            Disposer.dispose((jdk as Disposable))
                        }
                    }
                    removedSdks.forEach { sdk -> SdkConfigurationUtil.addSdk(sdk) }
                    removedSdks.clear()
                }
            },
            ThrowableRunnable {
                Messages.setTestDialog(TestDialog.DEFAULT)
                deleteBuildSystemDirectory()
                // was FileUtil.delete(BuildManager.getInstance().buildSystemDirectory.toFile())
                sdkCreationChecker?.removeNewKotlinSdk()
            },
            ThrowableRunnable {
                super.tearDown()
            }
        ).run()
    }

    override fun collectAllowedRoots(roots: MutableList<String>) {
        super.collectAllowedRoots(roots)
        roots.add(myJdkHome)
        roots.addAll(ExternalSystemTestCase.collectRootsInside(myJdkHome))
        roots.add(PathManager.getConfigPath())
    }

    protected open fun collectAllowedRoots(
        roots: MutableList<String>,
        distribution: PathAssembler.LocalDistribution?
    ) {
        //Note: could be required to use:
        //Environment.getEnvVariable("JAVA_HOME")
        roots.add(myJdkHome)
    }

    override fun getName(): String {
        return if (name.methodName == null) super.getName() else FileUtil.sanitizeFileName(name.methodName)
    }

    override fun getExternalSystemConfigFileName(): String = "build.gradle"

    @Throws(IOException::class)
    protected open fun importProjectUsingSingeModulePerGradleProject(config: String? = null, skipIndexing: Boolean? = null) {
        currentExternalProjectSettings.isResolveModulePerSourceSet = false
        importProject(config, skipIndexing)
    }

    open fun importProject() {
        importProject(skipIndexing = true)
    }

    override fun importProject(skipIndexing: Boolean?) {
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
        super.importProject(skipIndexing)
    }

    @Throws(IOException::class)
    override fun importProject(@NonNls @Language("Groovy") config: String?, skipIndexing: Boolean?) {
        var config = config
        config = injectRepo(config)
        super.importProject(config, skipIndexing)
    }

    override fun handleImportFailure(errorMessage: String, errorDetails: String?) {
        val gradleOutput = GradleProcessOutputInterceptor.getInstance()?.getOutput().orEmpty()

        // Typically Gradle error message consists of a line with the description of the error followed by
        // a multi-line stacktrace. The idea is to cut off the stacktrace if it is already contained in
        // the intercepted Gradle process output to avoid unnecessary verbosity.
        val compactErrorMessage = when (val indexOfNewLine = errorMessage.indexOf('\n')) {
            -1 -> errorMessage
            else -> {
                val compactErrorMessage = errorMessage.substring(0, indexOfNewLine)
                val theRest = errorMessage.substring(indexOfNewLine + 1)
                if (theRest in gradleOutput) compactErrorMessage else errorMessage
            }
        }

        val failureMessage = buildString {
            append("Gradle import failed: ").append(compactErrorMessage).append('\n')
            if (!errorDetails.isNullOrBlank()) append("Error details: ").append(errorDetails).append('\n')
            append("Gradle process output (BEGIN):\n")
            append(gradleOutput)
            if (!gradleOutput.endsWith('\n')) append('\n')
            append("Gradle process output (END)")
        }
        fail(failureMessage)
    }

    protected open fun injectRepo(@NonNls @Language("Groovy") config: String?): String {
        var config = config ?: ""
        config = """allprojects {
          repositories {
            maven {
                url 'https://repo.labs.intellij.net/repo1'
            }
        }}
        $config"""
        return config
    }

    override fun createImportSpec(): ImportSpec? {
        val importSpecBuilder = ImportSpecBuilder(super.createImportSpec())
        importSpecBuilder.withArguments("--stacktrace")
        return importSpecBuilder.build()
    }

    override fun getCurrentExternalProjectSettings(): GradleProjectSettings = myProjectSettings

    override fun getExternalSystemId(): ProjectSystemId = GradleConstants.SYSTEM_ID

    @Throws(IOException::class)
    protected open fun createSettingsFile(@NonNls @Language("Groovy") content: String?): VirtualFile? {
        return createProjectSubFile("settings.gradle", content)
    }

    protected open fun testDataDirName(): String = ""

    protected open fun testDataDirectory(): File {
        val baseDir = "${PluginTestCaseBase.getTestDataPathBase()}/gradle/${testDataDirName()}/"
        return File(baseDir, getTestName(true).substringBefore("_"))
    }

    protected fun configureKotlinVersionAndProperties(text: String, properties: Map<String, String>? = null): String {
        var result = text
        (properties ?: getDefaultPropertiesMap()).forEach { (key, value) ->
            result = result.replace("{{${key}}}", value)
        }
        return result
    }

    protected open fun configureByFiles(properties: Map<String, String>? = null): List<VirtualFile> {
        val rootDir = testDataDirectory()
        assert(rootDir.exists()) { "Directory ${rootDir.path} doesn't exist" }

        val unitedProperties = HashMap(properties ?: emptyMap()).apply { putAll(getDefaultPropertiesMap()) }

        return rootDir.walk().mapNotNull {
            when {
                it.isDirectory -> null

                !it.name.endsWith(SUFFIX) -> {
                    val text =
                        configureKotlinVersionAndProperties(FileUtil.loadFile(it, /* convertLineSeparators = */ true), unitedProperties)
                    val virtualFile = createProjectSubFile(it.path.substringAfter(rootDir.path + File.separator), text)

                    // Real file with expected testdata allows to throw nicer exceptions in
                    // case of mismatch, as well as open interactive diff window in IDEA
                    virtualFile.putUserData(VfsTestUtil.TEST_DATA_FILE_PATH, it.absolutePath)

                    virtualFile
                }

                else -> null
            }
        }.toList()
    }

    protected fun importProjectFromTestData(skipIndexing: Boolean? = null): List<VirtualFile> {
        val files = configureByFiles()
        importProject(skipIndexing)
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
                val actualText = configureKotlinVersionAndProperties(LoadTextUtil.loadText(it).toString())
                val expectedFileName = if (File(testDataDirectory(), it.name + ".$gradleVersion" + SUFFIX).exists()) {
                    it.name + ".$gradleVersion" + SUFFIX
                } else {
                    it.name + SUFFIX
                }
                KotlinTestUtils.assertEqualsToFile(File(testDataDirectory(), expectedFileName), actualText)
                { s -> configureKotlinVersionAndProperties(s) }
            }
    }

    protected open fun enableGradleDebugWithSuspend() {
        GradleSystemSettings.getInstance().gradleVmOptions = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
    }

    @Throws(IOException::class, URISyntaxException::class)
    private fun configureWrapper(): PathAssembler.LocalDistribution {
        val distributionUri = DistributionLocator().getDistributionFor(GradleVersion.version(gradleVersion))

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

        val wrapperConfiguration =
            GradleUtil.getWrapperConfiguration(projectPath)
        val localDistribution = PathAssembler(
            StartParameter.DEFAULT_GRADLE_USER_HOME
        ).getDistribution(wrapperConfiguration)

        val zip = localDistribution.zipFile
        try {
            if (zip.exists()) {
                val zipFile = ZipFile(zip)
                zipFile.close()
            }
        } catch (e: ZipException) {
            e.printStackTrace()
            println("Corrupted file will be removed: " + zip.path)
            FileUtil.delete(zip)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return localDistribution
    }

    private fun runWrite(f: () -> Unit) {
        object : WriteAction<Any>() {
            override fun run(result: Result<Any>) {
                f()
            }
        }.execute()
    }

    private fun getDefaultPropertiesMap(): Map<String, String> {
        val defaultProperties = HashMap<String, String>()
        defaultProperties["kotlin_plugin_version"] = gradleKotlinPluginVersion
        defaultProperties["kotlin_plugin_repositories"] = repositories()
        defaultProperties["kts_kotlin_plugin_repositories"] = repositories()

        return defaultProperties
    }

    private fun repositories(): String {
        return """
            mavenLocal()
            mavenCentral()
            gradlePluginPortal()
            google()
            jcenter()
        """.trimIndent()
    }

    private fun wrapperJar(): File {
        return File(PathUtil.getJarPathForClass(GradleWrapperMain::class.java))
    }

    companion object {
        const val GRADLE_JDK_NAME = "Gradle JDK"
        private const val GRADLE_DAEMON_TTL_MS = 10000

        @JvmStatic
        protected val SUFFIX = ".after"

        const val RELEASED_A_YEAR_AGO_GRADLE_PLUGIN_VERSION = "1.3.72"
        const val LATEST_STABLE_RELEASE_GRADLE_PLUGIN_VERSION = "1.4.30"

        val MASTER_VERSION_OF_PLUGIN
            get() = File("libraries/tools/kotlin-gradle-plugin/build/libs").listFiles()?.map { it.name }
                ?.firstOrNull { it.contains("-original.jar") }?.replace(
                    "kotlin-gradle-plugin-",
                    ""
                )?.replace("-original.jar", "") ?: "1.5.255-SNAPSHOT"

        const val LATEST_SUPPORTED_GRADLE_VERSION = "6.5.1"

        val SUPPORTED_GRADLE_VERSIONS = arrayOf("4.9", "6.1.1", LATEST_SUPPORTED_GRADLE_VERSION)

        private val LOCAL_RUN_PARAMS: Array<Any> =
            System.getenv("IMPORTING_TESTS_LOCAL_RUN_PARAMS") // You can specify versions for local run. For example: "6.7.1:1.4.30"
                ?.replace(" ", "")
                ?.split(":")?.toTypedArray()
                ?: arrayOf(LATEST_SUPPORTED_GRADLE_VERSION, "master")

        private val isTeamcityBuild = UsefulTestCase.IS_UNDER_TEAMCITY

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: Gradle-{0}, KotlinGradlePlugin-{1}")
        fun data(): Collection<Array<Any>> {
            if (isTeamcityBuild)
                return (SUPPORTED_GRADLE_VERSIONS).flatMap { gradleVersion ->
                    arrayOf(
                        RELEASED_A_YEAR_AGO_GRADLE_PLUGIN_VERSION,
                        LATEST_STABLE_RELEASE_GRADLE_PLUGIN_VERSION,
                        "master"
                    ).map { pluginVersion ->
                        arrayOf(gradleVersion, pluginVersion)
                    }
                }
            return listOf(LOCAL_RUN_PARAMS)
        }
    }
}