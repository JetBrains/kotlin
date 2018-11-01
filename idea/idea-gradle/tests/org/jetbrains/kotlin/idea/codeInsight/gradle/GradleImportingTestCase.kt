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
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
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
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.KotlinResourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinStatus
import org.jetbrains.kotlin.idea.configuration.ModuleSourceRootMap
import org.jetbrains.kotlin.idea.configuration.allConfigurators
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.GroovyFileType
import org.junit.Assert
import org.junit.Assume.assumeThat
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.lang.IllegalArgumentException
import java.net.URISyntaxException
import java.util.*

// part of org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
@RunWith(value = Parameterized::class)
abstract class GradleImportingTestCase : ExternalSystemImportingTestCase() {

    @JvmField
    @Rule
    var name = TestName()

    @JvmField
    @Rule
    var versionMatcherRule = VersionMatcherRule()

    @JvmField
    @Parameterized.Parameter
    var gradleVersion: String = ""

    private lateinit var myProjectSettings: GradleProjectSettings
    private lateinit var myJdkHome: String

    override fun setUp() {
        myJdkHome = IdeaTestUtil.requireRealJdkHome()
        super.setUp()
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
        myProjectSettings = GradleProjectSettings()
        GradleSettings.getInstance(myProject).gradleVmOptions = "-Xmx128m -XX:MaxPermSize=64m"
        System.setProperty(ExternalSystemExecutionSettings.REMOTE_PROCESS_IDLE_TTL_IN_MS_KEY, GRADLE_DAEMON_TTL_MS.toString())
        configureWrapper()
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
        } finally {
            super.tearDown()
        }
    }

    /**
     * Creates test project, consisting of:
     * - file at [projectDirPath]
     * - all other files in the same directory and with the same name as the main build file
     * - destination of those files in test project is determined *by the directives in file content*, not by its physical location
     *   (see [loadFileIntoTestProject])
     */
    open fun loadProject(projectDirPath: String) {
        val projectDir = File(projectDirPath)
        require(projectDir.isDirectory) { "${projectDir.absolutePath} is not a folder" }

        val buildFile = projectDir.resolve("build.gradle")
        require(buildFile.exists()) { "${buildFile.absolutePath} not found" }

        projectDir.walk().filter { it.isFile }.forEach {
            val relativePathInProject = it.relativeTo(projectDir).path
            loadFileIntoTestProject(it, relativePathInProject)
        }

        val configuration = loadTestConfiguration(buildFile)
        myProjectSettings.isResolveModulePerSourceSet = !configuration.singleModulePerSourceSet

        importProject()

        assertAllModulesConfigured()
    }

    private fun assertAllModulesConfigured() {
        runReadAction {
            for (moduleGroup in ModuleSourceRootMap(myProject).groupByBaseModules(myProject.allModules())) {
                val configurator = allConfigurators().find {
                    it.getStatus(moduleGroup) == ConfigureKotlinStatus.CAN_BE_CONFIGURED
                }
                Assert.assertNull("Configurator $configurator tells that ${moduleGroup.baseModule} can be configured", configurator)
            }
        }
    }

    data class FacetImportingTestConfiguration(val singleModulePerSourceSet: Boolean)

    private fun loadTestConfiguration(file: File): FacetImportingTestConfiguration {
        val fileText = KotlinTestUtils.doLoadFile(file)
        val singleModulePerSourceSet = fileText.lines().any { it.startsWith("// !SINGLE_MODULE_PER_SOURCE_SET") }
        return FacetImportingTestConfiguration(singleModulePerSourceSet)
    }

    private fun loadFileIntoTestProject(file: File, targetRelativePath: String) {
        val buildFileText = KotlinTestUtils.doLoadFile(file)

        val processedFileText = runPreprocessor(buildFileText)

        createProjectSubFile(targetRelativePath, processedFileText)
    }

    private fun runPreprocessor(fileText: String): String {
        return fileText.replace("\$\$ANDROID_SDK\$\$", KotlinTestUtils.getAndroidSdkSystemIndependentPath())
    }

    open fun checkFacet(projectDirPath: String, module: Module) {
        val actualFacet = KotlinFacet.get(module)
        // TODO. More accurate check: either
        //  - render absent facet in a file (i.e. make 'module'-'.facet' a 1-1 correspondence)
        //  - do not render absent facet (but then check that we haven't expected it to be present, i.e. that there's no '.facet'-file)
        if (actualFacet == null) return

        val expectedFacetFile = File(projectDirPath).resolve("${actualFacet.module.name}.facet")
        val actualRenderedFacet = buildString {
            val printer = Printer(this)
            renderFacet(actualFacet, printer)

            printer.println("Libraries:")
            printer.pushIndent()
            renderLibraries(module, printer)
            printer.popIndent()

            printer.println("hasKotlinSdk=${module.sdk?.sdkType is KotlinSdkType}")
        }
        KotlinTestUtils.assertEqualsToFile(expectedFacetFile, actualRenderedFacet)
    }

    private fun renderLibraries(module: Module, printer: Printer) {
        val rootManager = ModuleRootManager.getInstance(module)
        val libraries = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().mapNotNull { it.library }

        for (library in libraries) {
            printer.println(
                "name=${library.name}, " +
                        "kind=${(library as? LibraryEx)?.kind ?: "Can't get LibraryKind"}, " +
                        "hasFiles=${library.getFiles(OrderRootType.CLASSES).isNotEmpty()}"
            )
        }
    }

    fun renderFacet(facet: KotlinFacet, printer: Printer) {
        val facetSettings = facet.configuration.settings

        with(printer) {
            println("moduleName=${facet.module.name}")
            println("languageLevel=${facetSettings.languageLevel}")
            println("apiLevel=${facetSettings.apiLevel}")
            println("platform=${facetSettings.platform}")

            println("implementedModuleNames=${facetSettings.implementedModuleNames}")

            println("compilerArguments=${facetSettings.compilerArguments?.javaClass?.simpleName}")
            pushIndent()
            renderCompilerArguments(facetSettings.compilerArguments, printer)
            popIndent()

            println("compilerSettings")
            pushIndent()
            renderCompilerSettings(facetSettings.compilerSettings, printer)
            popIndent()

            println("sourceRoots")
            pushIndent()
            renderSourceRootInfos(printer, facet.module.name)
            popIndent()
        }
    }

    private fun renderCompilerSettings(compilerSettings: CompilerSettings?, printer: Printer) {
        if (compilerSettings == null) return

        fun Printer.printIfNonDefault(value: Any?, defaultValue: Any?) {
            if (value != defaultValue) println(value)
        }

        with(printer) {
            printIfNonDefault(compilerSettings.additionalArguments, CompilerSettings.DEFAULT_ADDITIONAL_ARGUMENTS)
            printIfNonDefault(compilerSettings.copyJsLibraryFiles, true)
            printIfNonDefault(compilerSettings.outputDirectoryForJsLibraryFiles, CompilerSettings.DEFAULT_OUTPUT_DIRECTORY)
            printIfNonDefault(compilerSettings.scriptTemplates, "")
            printIfNonDefault(compilerSettings.scriptTemplatesClasspath, "")
        }
    }

    private fun renderCompilerArguments(compilerArguments: CommonCompilerArguments?, printer: Printer) {
        if (compilerArguments == null) return
        printer.println(ArgumentUtils.convertArgumentsToStringList(compilerArguments).map { it.replace(projectPath, "") })
    }

    private fun renderSourceRootInfos(printer: Printer, moduleName: String) {
        fun JpsModuleSourceRootType<*>.render(): String = when (this) {
            JavaSourceRootType.SOURCE -> "JavaSourceRootType.SOURCE"
            JavaSourceRootType.TEST_SOURCE -> "JavaSourceRootType.TEST_SOURCE"
            JavaResourceRootType.RESOURCE -> "JavaResourceRootType.RESOURCE"
            JavaResourceRootType.TEST_RESOURCE -> "JavaResourceRootType.TEST_RESOURCE"
            KotlinSourceRootType.Source -> "KotlinSourceRootType.Source"
            KotlinSourceRootType.TestSource -> "KotlinSourceRootType.TestSource"
            KotlinResourceRootType.Resource -> "KotlinResourceRootType.Resource"
            KotlinResourceRootType.TestResource -> "KotlinResourceRootType.TestResource"
            else -> throw IllegalArgumentException("Unknown JpsModuleSourceRootType: ${this}")
        }

        with(printer) {
            val sourceRoots = getSourceRootInfos(moduleName).sortedBy { (relativePath, _) -> relativePath }
            for ((relativePath, sourceRootType) in sourceRoots) {
                println("$relativePath -> ${sourceRootType.render()}")
            }
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
                            url 'http://maven.labs.intellij.net/repo1'
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

    protected fun configureByFiles(): List<VirtualFile> {
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

    private fun runWrite(f: () -> Unit) {
        object : WriteAction<Any>() {
            override fun run(result: Result<Any>) {
                f()
            }
        }.execute()
    }

    protected fun facetSettings(moduleName: String) = kotlinFacet(moduleName)!!.configuration.settings

    protected fun kotlinFacet(moduleName: String) =
        KotlinFacet.get(getModule(moduleName))

    protected fun getSourceRootInfos(moduleName: String): List<Pair<String, JpsModuleSourceRootType<*>>> {
        return ModuleRootManager.getInstance(getModule(moduleName)).contentEntries.flatMap {
            it.sourceFolders.map { it.url.replace(projectPath, "") to it.rootType }
        }
    }

    protected val facetSettings: KotlinFacetSettings
        get() = facetSettings("project_main")


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
    }
}
