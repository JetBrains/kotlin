/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.codegen

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.TestsCompilerError
import org.jetbrains.kotlin.TestsCompiletimeError
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.Companion.createForTests
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.JvmTarget.Companion.fromString
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil.getFileClassInfoNoResolve
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.getTestName
import org.jetbrains.kotlin.test.testFramework.disposeRootDisposable
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.util.KtTestUtil.getAnnotationsJar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.io.IOException
import java.lang.reflect.Method

abstract class CodegenTestCase {
    protected val testRootDisposable: Disposable = Disposer.newDisposable()
    protected lateinit var testInfo: TestInfo

    @JvmField
    protected var myEnvironment: KotlinCoreEnvironment? = null

    @JvmField
    protected var myFiles: CodegenTestFiles? = null

    @JvmField
    protected var classFileFactory: ClassFileFactory? = null

    @JvmField
    protected var initializedClassLoader: GeneratedClassLoader? = null

    protected var configurationKind: ConfigurationKind = ConfigurationKind.JDK_ONLY

    protected fun createConfiguration(
        kind: ConfigurationKind,
        jdkKind: TestJdkKind,
        classpath: List<File>,
    ): CompilerConfiguration {
        val configuration = KotlinTestUtils.newConfiguration(kind, jdkKind, classpath, emptyList())
        updateConfiguration(configuration)
        return configuration
    }

    protected fun createEnvironmentWithMockJdkAndIdeaAnnotations(configurationKind: ConfigurationKind) {
        checkTestInfrastructure(myEnvironment == null) { "must not set up myEnvironment twice" }
        val configuration = createConfiguration(
            configurationKind,
            TestJdkKind.MOCK_JDK,
            mutableListOf(getAnnotationsJar())
        )
        @OptIn(CoreEnvironmentDeprecation::class)
        myEnvironment = createForTests(
            this.testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        setupEnvironment(myEnvironment!!)
    }

    protected open fun setupEnvironment(environment: KotlinCoreEnvironment) {}

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        this.testInfo = testInfo
    }

    @AfterEach
    open fun tearDown() {
        myFiles = null
        myEnvironment = null
        classFileFactory = null

        if (initializedClassLoader != null) {
            initializedClassLoader!!.dispose()
            initializedClassLoader = null
        }

        disposeRootDisposable(testRootDisposable)
    }

    protected fun loadText(text: String) {
        myFiles = CodegenTestFiles.create("$DEFAULT_TEST_FILE_NAME.kt", text, myEnvironment!!.project)
    }

    protected fun loadFile(@TestDataFile name: String): String {
        try {
            val file = File(KtTestUtil.getTestDataFileLocatedInCompilerTestData("codegen/$name").absolutePath)
            val content = FileUtil.loadFile(file, Charsets.UTF_8.name(), true)
            assert(myFiles == null) { "Should not initialize myFiles twice" }
            myFiles = CodegenTestFiles.create(file.getName(), content, myEnvironment!!.project)
            return content
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    protected fun loadFile() {
        loadFile(this.prefix + "/" + getTestName(testInfo) + ".kt")
    }

    protected open val prefix: String
        get() {
            throw UnsupportedOperationException()
        }

    protected fun generateAndCreateClassLoader(): GeneratedClassLoader {
        if (initializedClassLoader != null) {
            testInfraError("Double initialization of class loader in same test")
        }

        initializedClassLoader = createClassLoader()

        if (!CodegenTestUtil.verifyAllFilesWithAsm(/* factory = */ generateClassesInFile(), /* reportProblems = */ true)) {
            testInfraError("Verification failed: see exceptions above")
        }

        return initializedClassLoader!!
    }

    protected fun createClassLoader(): GeneratedClassLoader {
        val classLoader = if (configurationKind.withReflection) {
            ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
        } else {
            ForTestCompileRuntime.runtimeJarClassLoader()
        }

        return GeneratedClassLoader(generateClassesInFile(), classLoader)
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
            return generateAndCreateClassLoader().loadClass(name)
        } catch (_: ClassNotFoundException) {
            testInfraError("No class file was generated for: $name")
        }
    }

    protected fun generateClassesInFile(): ClassFileFactory {
        if (classFileFactory != null) return classFileFactory!!

        try {
            val generationState = GenerationUtils.compileFiles(
                myFiles!!.psiFiles, myEnvironment!!, ClassBuilderFactories.TEST
            )
            classFileFactory = generationState.factory

            // Some names are not allowed in the dex file format and the VM will reject the program
            // if they are used. Therefore, a few tests cannot be dexed as they use such names that
            // are valid on the JVM but not on the Android Runtime.
            val ignoreDexing = myFiles!!.psiFiles.any { InTextDirectivesUtils.isDirectiveDefined(it.getText(), "IGNORE_DEXING") }
            if (D8Checker.RUN_D8_CHECKER && !ignoreDexing) {
                D8Checker.check(classFileFactory)
            }
        } catch (e: TestsCompiletimeError) {
            e.original.printStackTrace()
            generateInstructionsAsText()
            System.err.println("See exceptions above")
            throw e
        } catch (e: Throwable) {
            generateInstructionsAsText()
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

    protected open fun updateConfiguration(configuration: CompilerConfiguration) {
        setCustomDefaultJvmTarget(configuration)
        configuration.useFir = true
        when (firParser) {
            FirParser.LightTree -> configuration.useLightTree = true
            FirParser.Psi -> {}
        }
    }

    protected abstract val firParser: FirParser

    companion object {
        private const val DEFAULT_TEST_FILE_NAME = "a_test"
        private val DEFAULT_JVM_TARGET: String? = System.getProperty("kotlin.test.default.jvm.target")


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
    }
}
