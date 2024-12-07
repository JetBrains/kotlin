/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import junit.framework.TestCase
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.codegen.extractUrls
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.clientserver.TestProxy
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.ATTACH_DEBUGGER
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ENABLE_JVM_PREVIEW
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.PREFER_IN_TEST_OVER_STDLIB
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.model.nameWithoutExtension
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator.Companion.TEST_CONFIGURATION_KIND_KEY
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.services.jvm.jvmBoxMainClassProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.fileContainsBoxMethod
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit

open class JvmBoxRunner(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    companion object {
        private val BOX_IN_SEPARATE_PROCESS_PORT = System.getProperty("kotlin.test.box.in.separate.process.port")
        private const val DEFAULT_EXPECTED_RESULT = "OK"
        private const val OUTPUT_EXTENSION = "box.txt"
    }

    private var boxMethodFound = false

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!boxMethodFound) {
            assertions.fail { "Can't find box methods" }
        }
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val fileInfos = info.fileInfos.ifEmpty { return }
        val reportProblems = !testServices.codegenSuppressionChecker.failuresInModuleAreIgnored(module)
        val classLoader = createAndVerifyClassLoader(module, info.classFileFactory, reportProblems)
        try {
            for (fileInfo in fileInfos) {
                if (fileContainsBoxMethod(fileInfo.sourceFile)) {
                    boxMethodFound = true
                    callBoxMethodAndCheckResultWithCleanup(
                        fileInfo.info,
                        module,
                        info.classFileFactory,
                        classLoader,
                        unexpectedBehaviour = false,
                        reportProblems = reportProblems
                    )
                    return
                }
            }
        } finally {
            classLoader.dispose()
        }
    }

    private fun callBoxMethodAndCheckResultWithCleanup(
        fileInfo: JvmFileClassInfo,
        module: TestModule,
        classFileFactory: ClassFileFactory,
        classLoader: URLClassLoader,
        unexpectedBehaviour: Boolean,
        reportProblems: Boolean
    ) {
        try {
            callBoxMethodAndCheckResult(fileInfo, module, classFileFactory, classLoader, unexpectedBehaviour)
        } catch (e: Throwable) {
            if (reportProblems) {
                try {
                    println(classFileFactory.createText())
                } catch (_: Throwable) {
                    // In FIR we have factory which can't print bytecode
                    //   and it throws exception otherwise. So we need
                    //   ignore that exception to report original one
                    // TODO: fix original problem
                }
            }
            throw e
        }
    }

    private fun findClassAndMethodToExecute(
        fileInfo: JvmFileClassInfo,
        classLoader: URLClassLoader,
        classFileFactory: ClassFileFactory
    ): Pair<Class<*>, Method> {
        val className = fileInfo.facadeClassFqName.asString()
        val clazz = try {
            classLoader.getGeneratedClass(className)
        } catch (e: LinkageError) {
            throw AssertionError("Failed to load class '$className':\n${classFileFactory.createText()}", e)
        }
        val method = clazz.getBoxMethodOrNull() ?: error("box method not found")
        return clazz to method
    }

    private fun callBoxMethodAndCheckResult(
        fileInfo: JvmFileClassInfo,
        module: TestModule,
        classFileFactory: ClassFileFactory,
        classLoader: URLClassLoader,
        unexpectedBehaviour: Boolean
    ) {
        val result = if (BOX_IN_SEPARATE_PROCESS_PORT != null) {
            invokeBoxInSeparateProcess(
                module,
                classFileFactory,
                classLoader,
                findClassAndMethodToExecute(fileInfo, classLoader, classFileFactory).first
            )
        } else {
            val jdkKind = module.directives.singleOrZeroValue(JDK_KIND)
            if (jdkKind?.requiresSeparateProcess == true || REQUIRES_SEPARATE_PROCESS in module.directives) {
                runSeparateJvmInstance(module, jdkKind ?: TestJdkKind.FULL_JDK, classLoader, classFileFactory)
            } else {
                runBoxInCurrentProcess(
                    classLoader,
                    findClassAndMethodToExecute(fileInfo, classLoader, classFileFactory).second
                )
            }
        }
        if (unexpectedBehaviour) {
            TestCase.assertNotSame(DEFAULT_EXPECTED_RESULT, result)
        } else {
            val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
            val outputFile = originalFile.withExtension(OUTPUT_EXTENSION)
            if (outputFile.exists()) {
                assertions.assertEqualsToFile(outputFile, result)
            } else {
                assertions.assertEquals(DEFAULT_EXPECTED_RESULT, result)
            }
        }
    }

    private fun runBoxInCurrentProcess(
        classLoader: URLClassLoader,
        method: Method,
    ): String {
        val savedClassLoader = Thread.currentThread().contextClassLoader
        if (savedClassLoader !== classLoader) {
            // otherwise the test infrastructure used in the test may conflict with the one from the context classloader
            Thread.currentThread().contextClassLoader = classLoader
        }
        return try {
            method.invoke(null) as String
        } finally {
            if (savedClassLoader !== classLoader) {
                Thread.currentThread().contextClassLoader = savedClassLoader
            }
        }
    }

    /*
     * TODO:
     * Running separate jvm for each test may be very expensive in case
     *   there will be a lot of tests which use this feature, so we should
     *   consider to run single jvm as proxy (see [invokeBoxInSeparateProcess])
     */
    private fun runSeparateJvmInstance(
        module: TestModule,
        jdkKind: TestJdkKind,
        classLoader: URLClassLoader,
        classFileFactory: ClassFileFactory
    ): String {
        val jdkHome = when (jdkKind) {
            TestJdkKind.FULL_JDK -> KtTestUtil.getJdk8Home()
            TestJdkKind.FULL_JDK_11 -> KtTestUtil.getJdk11Home()
            TestJdkKind.FULL_JDK_17 -> KtTestUtil.getJdk17Home()
            TestJdkKind.FULL_JDK_21 -> KtTestUtil.getJdk21Home()
            else -> error("Unsupported JDK kind: $jdkKind")
        }

        val javaExe = File(jdkHome, "bin/java.exe").takeIf(File::exists)
            ?: File(jdkHome, "bin/java").takeIf(File::exists)
            ?: error("Can't find 'java' executable in $jdkHome")

        val classPath = extractClassPath(module, classLoader, classFileFactory)

        val mainClassAndArguments = testServices.jvmBoxMainClassProvider?.getMainClassNameAndAdditionalArguments(module) ?: run {
            val mainFile = module.files.firstOrNull {
                it.name == MainFunctionForBlackBoxTestsSourceProvider.BOX_MAIN_FILE_NAME && it.isAdditional
            } ?: error("No file with main function was generated. Please check TODO source provider")

            val mainFqName = listOfNotNull(
                MainFunctionForBlackBoxTestsSourceProvider.detectPackage(mainFile),
                "${mainFile.nameWithoutExtension}Kt"
            ).joinToString(".")

            listOf(mainFqName)
        }

        val process = launchSeparateJvmProcess(javaExe, module, classPath, mainClassAndArguments)
        process.waitFor(3, TimeUnit.MINUTES)
        return try {
            when (process.exitValue()) {
                0 -> "OK"
                else -> buildString {
                    for (stream in listOfNotNull(process.inputStream, process.errorStream)) {
                        stream.bufferedReader().lines().forEach { appendLine(it) }
                    }

                    if (this.isEmpty()) {
                        appendLine("External process completed with error. Check the build log")
                    }
                }
            }
        } finally {
            process.outputStream.flush()
        }
    }

    protected open fun launchSeparateJvmProcess(
        javaExe: File,
        module: TestModule,
        classPath: List<URL>,
        mainClassAndArguments: List<String>
    ): Process {
        val command = listOfNotNull(
            javaExe.absolutePath,
            runIf(ATTACH_DEBUGGER in module.directives) { "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005" },
            "-ea",
            runIf(ENABLE_JVM_PREVIEW in module.directives) { "--enable-preview" },
            "-classpath",
            classPath.joinToString(File.pathSeparator, transform = { File(it.toURI()).absolutePath }),
        ) + mainClassAndArguments

        return ProcessBuilder(command).start()
    }

    private fun invokeBoxInSeparateProcess(
        module: TestModule,
        classFileFactory: ClassFileFactory,
        classLoader: URLClassLoader,
        clazz: Class<*>
    ): String {
        val classPath = extractClassPath(module, classLoader, classFileFactory)
        val proxy = TestProxy(Integer.valueOf(BOX_IN_SEPARATE_PROCESS_PORT), clazz.canonicalName, classPath)
        return proxy.runTest()
    }

    private fun extractClassPath(
        module: TestModule,
        classLoader: URLClassLoader,
        classFileFactory: ClassFileFactory
    ): List<URL> {
        return buildList {
            if (classLoader is GeneratedClassLoader) {
                add(testServices.compiledClassesManager.compileKotlinToDiskAndGetOutputDir(module, classFileFactory).url)
            }
            addAll(classLoader.extractUrls())
        }
    }

    private val File.url: URL
        get() = toURI().toURL()

    private fun createAndVerifyClassLoader(
        module: TestModule,
        classFileFactory: ClassFileFactory,
        reportProblems: Boolean
    ): GeneratedClassLoader {
        val classLoader = generatedTestClassLoader(testServices, module, classFileFactory)
        if (REQUIRES_SEPARATE_PROCESS !in module.directives && module.directives.singleOrZeroValue(JDK_KIND)?.requiresSeparateProcess != true) {
            val verificationSucceeded = CodegenTestUtil.verifyAllFilesWithAsm(classFileFactory, reportProblems)
            if (!verificationSucceeded) {
                assertions.fail { "Verification failed: see exceptions above" }
            }
        }
        return classLoader
    }

    private fun ClassLoader.getGeneratedClass(className: String): Class<*> {
        try {
            return loadClass(className)
        } catch (e: ClassNotFoundException) {
            assertions.fail { "No class file was generated for: $className" }
        }
    }

    private fun Class<*>.getBoxMethodOrNull(): Method? {
        return try {
            getMethod("box")
        } catch (e: NoSuchMethodException) {
            return null
        }
    }
}

internal fun generatedTestClassLoader(
    testServices: TestServices,
    module: TestModule,
    classFileFactory: ClassFileFactory,
): GeneratedClassLoader {
    val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
    val classpath = computeTestRuntimeClasspath(testServices, module)
    if (PREFER_IN_TEST_OVER_STDLIB in module.directives) {
        val libPathProvider = testServices.standardLibrariesPathProvider
        classpath += libPathProvider.runtimeJarForTests()
        if (configuration[TEST_CONFIGURATION_KIND_KEY]?.withReflection == true) {
            classpath += libPathProvider.reflectJarForTests()
        }
        classpath += libPathProvider.scriptRuntimeJarForTests()
        classpath += libPathProvider.kotlinTestJarForTests()
        return GeneratedClassLoader(classFileFactory, null, *(classpath.map { it.toURI().toURL() }.toTypedArray()))
    } else {
        val parentClassLoader = if (configuration[TEST_CONFIGURATION_KIND_KEY]?.withReflection == true) {
            testServices.standardLibrariesPathProvider.getRuntimeAndReflectJarClassLoader()
        } else {
            testServices.standardLibrariesPathProvider.getRuntimeJarClassLoader()
        }
        return GeneratedClassLoader(classFileFactory, parentClassLoader, *classpath.map { it.toURI().toURL() }.toTypedArray())
    }
}

private fun computeTestRuntimeClasspath(testServices: TestServices, rootModule: TestModule): MutableList<File> {
    val visited = mutableSetOf<TestModule>()
    val result = mutableListOf<File>()

    fun computeClasspath(module: TestModule, isRoot: Boolean) {
        if (!visited.add(module)) return

        if (isRoot) {
            // Add output dir to the classpath in case there are Java compiled classes in the current module.
            result.addIfNotNull(testServices.compiledClassesManager.getOutputDirForModule(module))
        } else {
            result.add(testServices.compiledClassesManager.compileKotlinToDiskAndGetOutputDir(module, classFileFactory = null))
        }

        for (dependency in module.allDependencies) {
            if (dependency.kind == DependencyKind.Binary) {
                computeClasspath(testServices.dependencyProvider.getTestModule(dependency.moduleName), false)
            }
        }
    }

    computeClasspath(rootModule, true)
    testServices.runtimeClasspathProviders.flatMapTo(result) { it.runtimeClassPaths(rootModule) }
    return result
}
