/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.codegen

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.TestDataFile
import junit.framework.TestCase
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.TestsCompilerError
import org.jetbrains.kotlin.TestsCompiletimeError
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.Companion.createForTests
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.JvmTarget.Companion.fromString
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil.getFileClassInfoNoResolve
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.K1SpecificScriptingServiceAccessor
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

abstract class CodegenTestCase : KotlinBaseTest<KotlinBaseTest.TestFile>() {
    @JvmField
    protected var myEnvironment: KotlinCoreEnvironment? = null

    @JvmField
    protected var myFiles: CodegenTestFiles? = null

    @JvmField
    protected var classFileFactory: ClassFileFactory? = null

    @JvmField
    protected var initializedClassLoader: GeneratedClassLoader? = null

    @JvmField
    protected var javaClassesOutputDirectory: File? = null
    protected var additionalDependencies: List<File> = emptyList()

    protected var configurationKind: ConfigurationKind = ConfigurationKind.JDK_ONLY

    protected fun createEnvironmentWithMockJdkAndIdeaAnnotations(
        configurationKind: ConfigurationKind,
        vararg javaSourceRoots: File
    ) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(
            configurationKind,
            mutableListOf(),
            TestJdkKind.MOCK_JDK,
            *javaSourceRoots
        )
    }

    protected fun createEnvironmentWithMockJdkAndIdeaAnnotations(
        configurationKind: ConfigurationKind,
        testFilesWithConfigurationDirectives: List<TestFile>,
        testJdkKind: TestJdkKind,
        vararg javaSourceRoots: File
    ) {
        check(myEnvironment == null) { "must not set up myEnvironment twice" }

        val configuration = createConfiguration(
            configurationKind,
            testJdkKind,
            mutableListOf<File>(KtTestUtil.getAnnotationsJar()),
            javaSourceRoots.toList(),
            testFilesWithConfigurationDirectives
        )

        myEnvironment = createForTests(
            testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        setupEnvironment(myEnvironment!!)
    }

    @Throws(Exception::class)
    override fun tearDown() {
        myFiles = null
        myEnvironment = null
        classFileFactory = null

        if (initializedClassLoader != null) {
            initializedClassLoader!!.dispose()
            initializedClassLoader = null
        }

        super.tearDown()
    }

    protected fun loadText(text: String) {
        myFiles = CodegenTestFiles.create("$DEFAULT_TEST_FILE_NAME.kt", text, myEnvironment!!.project)
    }

    protected fun loadFile(@TestDataFile name: String): String {
        return loadFileByFullPath(KtTestUtil.getTestDataFileLocatedInCompilerTestData("codegen/$name").absolutePath)
    }

    protected fun loadFileByFullPath(fullPath: String): String {
        try {
            val file = File(fullPath)
            val content = FileUtil.loadFile(file, Charsets.UTF_8.name(), true)
            assert(myFiles == null) { "Should not initialize myFiles twice" }
            myFiles = CodegenTestFiles.create(file.getName(), content, myEnvironment!!.project)
            return content
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    protected fun loadFiles(vararg names: String) {
        val files: MutableList<KtFile?> = ArrayList(names.size)
        for (name in names) {
            try {
                val content = KtTestUtil.doLoadFile(KtTestUtil.getTestDataFileLocatedInCompilerTestData("codegen/").absolutePath, name)
                val file = KtTestUtil.createFile(name, content, myEnvironment!!.project)
                files.add(file)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        myFiles = CodegenTestFiles.create(files)
    }

    protected fun loadFile() {
        loadFile(this.prefix + "/" + getTestName(true) + ".kt")
    }

    protected open val prefix: String
        get() {
            throw UnsupportedOperationException()
        }

    protected fun generateAndCreateClassLoader(reportProblems: Boolean): GeneratedClassLoader {
        if (initializedClassLoader != null) {
            fail("Double initialization of class loader in same test")
        }

        initializedClassLoader = createClassLoader()

        if (!CodegenTestUtil.verifyAllFilesWithAsm(generateClassesInFile(reportProblems), reportProblems)) {
            fail("Verification failed: see exceptions above")
        }

        return initializedClassLoader!!
    }

    protected fun createClassLoader(): GeneratedClassLoader {
        val classLoader = if (configurationKind.withReflection) {
            ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
        } else {
            ForTestCompileRuntime.runtimeJarClassLoader()
        }

        return GeneratedClassLoader(
            generateClassesInFile(),
            classLoader,
            *this.classPathURLs
        )
    }

    private val classPathURLs: Array<URL?>
        get() {
            val files: MutableList<File?> = ArrayList()
            if (javaClassesOutputDirectory != null) {
                files.add(javaClassesOutputDirectory)
            }
            files.addAll(additionalDependencies)

            val environment = myEnvironment
            val externalImportsProvider: ScriptConfigurationsProvider? = environment?.configuration?.getCompilerExtensions(
                ScriptConfigurationsProvider
            )?.firstOrNull()
            if (externalImportsProvider != null) {
                environment.getSourceFiles().forEach(
                    Consumer { file: KtFile ->
                        @Suppress("DEPRECATION")
                        val refinedConfiguration = externalImportsProvider.getScriptConfiguration(environment.project, file)
                        if (refinedConfiguration != null) {
                            files.addAll(refinedConfiguration.dependenciesClassPath)
                        }
                    }
                )
            }

            try {
                val result = arrayOfNulls<URL>(files.size)
                for (i in files.indices) {
                    result[i] = files[i]!!.toURI().toURL()
                }
                return result
            } catch (e: MalformedURLException) {
                throw rethrow(e)
            }
        }

    protected fun generateToText(): String {
        if (classFileFactory == null) {
            classFileFactory = GenerationUtils.compileFiles(myFiles!!.psiFiles, myEnvironment!!).factory
        }
        return classFileFactory!!.createText(null)
    }

    protected fun generateFacadeClass(): Class<*> {
        val facadeClassFqName = getFileClassInfoNoResolve(myFiles!!.getPsiFile()).facadeClassFqName
        return generateClass(facadeClassFqName.asString())
    }

    protected fun generateClass(name: String): Class<*> {
        try {
            return generateAndCreateClassLoader(true).loadClass(name)
        } catch (_: ClassNotFoundException) {
            error("No class file was generated for: $name")
        }
    }

    protected fun generateClassesInFile(): ClassFileFactory {
        return generateClassesInFile(true)
    }

    private fun generateClassesInFile(reportProblems: Boolean): ClassFileFactory {
        if (classFileFactory != null) return classFileFactory!!

        try {
            val generationState = GenerationUtils.compileFiles(
                myFiles!!.psiFiles, myEnvironment!!, ClassBuilderFactories.TEST,
                NoScopeRecordCliBindingTrace(myEnvironment!!.project)
            )
            classFileFactory = generationState.factory

            // Some names are not allowed in the dex file format and the VM will reject the program
            // if they are used. Therefore, a few tests cannot be dexed as they use such names that
            // are valid on the JVM but not on the Android Runtime.
            val ignoreDexing = myFiles!!.psiFiles.stream()
                .anyMatch { it: KtFile? -> InTextDirectivesUtils.isDirectiveDefined(it!!.getText(), "IGNORE_DEXING") }
            if (D8Checker.RUN_D8_CHECKER && !ignoreDexing) {
                D8Checker.check(classFileFactory)
            }
        } catch (e: TestsCompiletimeError) {
            if (reportProblems) {
                e.original.printStackTrace()
                generateInstructionsAsText()
                System.err.println("See exceptions above")
            } else {
                System.err.println("Compilation failure")
            }
            throw e
        } catch (e: Throwable) {
            if (reportProblems) {
                generateInstructionsAsText()
            }
            throw TestsCompilerError(e)
        }
        return classFileFactory!!
    }

    private fun generateInstructionsAsText() {
        System.err.println("Generating instructions as text...")
        try {
            if (classFileFactory == null) {
                System.err.println("Cannot generate text: exception was thrown during generation")
            } else {
                System.err.println(classFileFactory!!.createText())
            }
        } catch (e1: Throwable) {
            System.err.println("Exception thrown while trying to generate text, the actual exception follows:")
            e1.printStackTrace()
            System.err.println("-----------------------------------------------------------------------------")
        }
    }

    protected fun generateFunction(): Method {
        val aClass = generateFacadeClass()
        try {
            return CodegenTestUtil.findTheOnlyMethod(aClass)
        } catch (e: Error) {
            println(generateToText())
            throw e
        }
    }

    protected fun generateFunction(name: String): Method {
        return CodegenTestUtil.findDeclaredMethodByName(generateFacadeClass(), name)
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        setCustomDefaultJvmTarget(configuration)
        configureIrFir(configuration)
    }

    protected fun compile(files: List<TestFile>, reportProblems: Boolean = true) {
        val javaSourceDir: File? = writeJavaFiles(files)

        configurationKind = extractConfigurationKind(files)

        val configuration = createConfiguration(
            configurationKind, getTestJdkKind(files),
            mutableListOf<File>(KtTestUtil.getAnnotationsJar()),
            arrayOf(javaSourceDir).filterNotNull(),
            files
        )

        myEnvironment = createForTests(
            testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        setupEnvironment(myEnvironment!!)

        myFiles = loadMultiFiles(files, myEnvironment!!.project)

        generateClassesInFile(reportProblems)

        val compileJavaFiles = javaSourceDir != null && javaClassesOutputDirectory == null
        var kotlinOut: File? = null

        // If there are Java files, they should be compiled against the class files produced by Kotlin, so we dump them to the disk
        if (compileJavaFiles) {
            kotlinOut = createTempDirectory(toString())
            classFileFactory!!.writeAllTo(kotlinOut)
        }

        javaClassesOutputDirectory = null
        if (compileJavaFiles) {
            javaClassesOutputDirectory = createTempDirectory("java-classes")
            val javacOptions: MutableList<String?> = extractJavacOptions(
                files,
                configuration[JVMConfigurationKeys.JVM_TARGET],
                configuration.getBoolean(JVMConfigurationKeys.ENABLE_JVM_PREVIEW)
            )
            val isJava9Module = false // No Java modules in legacy tests
            val finalJavacOptions = CodegenTestUtil.prepareJavacOptions(
                mutableListOf(kotlinOut?.path), javacOptions, javaClassesOutputDirectory!!, isJava9Module
            )

            compileJavaFiles(
                CodegenTestUtil.findJavaSourcesInDirectory(javaSourceDir).stream().map { pathname: String -> File(pathname) }
                    .collect(
                        Collectors.toList()
                    ),
                finalJavacOptions
            ).assertSuccessful()
        }
    }

    override val backend: TargetBackend
        get() = TargetBackend.JVM_IR

    override fun doTest(filePath: String) {
        val file = File(filePath)

        val expectedText = KtTestUtil.doLoadFile(file)
        val testFiles: MutableList<TestFile> = createTestFilesFromFile(file, expectedText)

        try {
            doMultiFileTest(file, testFiles)
        } catch (e: Exception) {
            throw rethrow(e)
        }
    }

    override fun createTestFilesFromFile(file: File, expectedText: String): MutableList<TestFile> {
        @OptIn(ObsoleteTestInfrastructure::class)
        return TestFiles.createTestFiles(
            file.getName(),
            expectedText,
            object : TestFiles.TestFileFactoryNoModules<TestFile>() {
                override fun create(
                    fileName: String,
                    text: String,
                    directives: Directives
                ): TestFile {
                    return TestFile(fileName, text, directives)
                }
            },
            false,
            parseDirectivesPerFiles()
        )
    }

    protected fun printReport(wholeFile: File) {
        val isIgnored = InTextDirectivesUtils.isIgnoredTarget(
            backend, wholeFile, *InTextDirectivesUtils.IGNORE_BACKEND_DIRECTIVE_PREFIXES
        )
        if (!isIgnored) {
            println(generateToText())
        }
    }

    companion object {
        private const val DEFAULT_TEST_FILE_NAME = "a_test"
        private val DEFAULT_JVM_TARGET: String? = System.getProperty("kotlin.test.default.jvm.target")

        private fun loadMultiFiles(files: List<TestFile>, project: Project): CodegenTestFiles {
            val ktFiles: MutableList<KtFile?> = ArrayList(files.size)
            for (file in files.sorted()) {
                if (file.name.endsWith(".kt") || file.name.endsWith(".kts")) {
                    // `rangesToDiagnosticNames` parameter is not-null only for diagnostic tests, it's using for lazy diagnostics
                    val content = CheckerTestUtil.parseDiagnosedRanges(file.content, ArrayList(0), null)
                    ktFiles.add(KtTestUtil.createFile(file.name, content, project))
                }
            }

            return CodegenTestFiles.create(ktFiles)
        }

        private fun setCustomDefaultJvmTarget(configuration: CompilerConfiguration) {
            if (DEFAULT_JVM_TARGET != null) {
                val customDefaultTarget: JvmTarget =
                    checkNotNull(fromString(DEFAULT_JVM_TARGET)) { "Can't construct JvmTarget for $DEFAULT_JVM_TARGET" }
                val originalTarget = configuration[JVMConfigurationKeys.JVM_TARGET]
                if (originalTarget == null || customDefaultTarget.majorVersion > originalTarget.majorVersion) {
                    // It's not safe to substitute target in general
                    // cause it can affect generated bytecode and original behaviour should be tested somehow.
                    // Original behaviour testing is perfomed by
                    //
                    //      codegenTest(target = 6, jvm = "Last", jdk = mostRecentJdk)
                    //      codegenTest(target = 8, jvm = "Last", jdk = mostRecentJdk)
                    //
                    // in compiler/tests-different-jdk/build.gradle.kts
                    configuration.put(JVMConfigurationKeys.JVM_TARGET, customDefaultTarget)
                }
            }
        }

        private fun extractJavacOptions(
            files: List<TestFile>,
            kotlinTarget: JvmTarget?,
            isJvmPreviewEnabled: Boolean
        ): MutableList<String?> {
            val javacOptions: MutableList<String?> = ArrayList(0)
            for (file in files) {
                javacOptions.addAll(InTextDirectivesUtils.findListWithPrefixes(file.content, "// JAVAC_OPTIONS:"))
            }

            if (kotlinTarget != null) {
                if (isJvmPreviewEnabled) {
                    javacOptions.add("--release")
                    javacOptions.add(kotlinTarget.description)
                    javacOptions.add("--enable-preview")
                } else {
                    javacOptions.add("-source")
                    javacOptions.add(kotlinTarget.description)
                    javacOptions.add("-target")
                    javacOptions.add(kotlinTarget.description)
                }
            }

            return javacOptions
        }

        private fun createTempDirectory(prefix: String?): File {
            try {
                return KtTestUtil.tmpDir(prefix)
            } catch (e: IOException) {
                throw rethrow(e)
            }
        }

        private fun writeJavaFiles(files: List<TestFile>): File? {
            val javaFiles = files.filter { file -> file.name.endsWith(".java") }
            if (javaFiles.isEmpty()) return null

            val dir: File = createTempDirectory("java-files")

            for (testFile in javaFiles) {
                val file = File(dir, testFile.name)
                KtTestUtil.mkdirs(file.getParentFile())
                file.writeText(testFile.content, Charsets.UTF_8)
            }

            return dir
        }

        @JvmStatic
        protected fun callBoxMethodAndCheckResult(classLoader: URLClassLoader?, method: Method, unexpectedBehaviour: Boolean) {
            val savedClassLoader = Thread.currentThread().getContextClassLoader()
            if (savedClassLoader !== classLoader) {
                // otherwise the test infrastructure used in the test may conflict with the one from the context classloader
                Thread.currentThread().setContextClassLoader(classLoader)
            }
            var result: String?
            try {
                result = runBoxMethod(method)
            } finally {
                if (savedClassLoader !== classLoader) {
                    Thread.currentThread().setContextClassLoader(savedClassLoader)
                }
            }
            if (unexpectedBehaviour) {
                assertNotSame("OK", result)
            } else {
                TestCase.assertEquals("OK", result)
            }
        }
    }
}
