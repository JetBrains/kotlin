/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import junit.framework.TestCase
import org.jetbrains.kotlin.backend.common.CodegenUtil.getMemberDeclarationsToGenerate
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.codegen.clearReflectionCache
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil.getFileClassInfoNoResolve
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator.Companion.TEST_CONFIGURATION_KIND_KEY
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.lang.reflect.Method
import java.net.URLClassLoader

class JvmBoxRunner(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    companion object {
        private val BOX_IN_SEPARATE_PROCESS_PORT = System.getProperty("kotlin.test.box.in.separate.process.port")
    }

    private var boxMethodFound = false

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!boxMethodFound) {
            assertions.fail { "Can't find box methods" }
        }
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val ktFiles = info.classFileFactory.inputFiles
        val reportProblems = module.targetBackend !in module.directives[CodegenTestDirectives.IGNORE_BACKEND]
        val classLoader = createAndVerifyClassLoader(module, info.classFileFactory, reportProblems)
        try {
            for (ktFile in ktFiles) {
                val className = ktFile.getFacadeFqName() ?: continue
                val clazz = classLoader.getGeneratedClass(className)
                val method = clazz.getBoxMethodOrNull() ?: continue
                boxMethodFound = true
                callBoxMethodAndCheckResultWithCleanup(
                    info.classFileFactory,
                    classLoader,
                    clazz,
                    method,
                    unexpectedBehaviour = false,
                    reportProblems = reportProblems
                )
                return
            }
        } finally {
            classLoader.dispose()
        }
    }

    private fun callBoxMethodAndCheckResultWithCleanup(
        factory: ClassFileFactory,
        classLoader: URLClassLoader,
        clazz: Class<*>?,
        method: Method,
        unexpectedBehaviour: Boolean,
        reportProblems: Boolean
    ) {
        try {
            callBoxMethodAndCheckResult(classLoader, clazz, method, unexpectedBehaviour)
        } catch (e: Throwable) {
            if (reportProblems) {
                try {
                    println(factory.createText())
                } catch (_: Throwable) {
                    // In FIR we have factory which can't print bytecode
                    //   and it throws exception otherwise. So we need
                    //   ignore that exception to report original one
                    // TODO: fix original problem
                }
            }
            throw e
        } finally {
            clearReflectionCache(classLoader)
        }
    }

    private fun callBoxMethodAndCheckResult(
        classLoader: URLClassLoader,
        @Suppress("UNUSED_PARAMETER") clazz: Class<*>?,
        method: Method,
        unexpectedBehaviour: Boolean
    ) {
        val result = if (BOX_IN_SEPARATE_PROCESS_PORT != null) {
            TODO()
//            result = invokeBoxInSeparateProcess(classLoader, clazz)
        } else {
            val savedClassLoader = Thread.currentThread().contextClassLoader
            if (savedClassLoader !== classLoader) {
                // otherwise the test infrastructure used in the test may conflict with the one from the context classloader
                Thread.currentThread().contextClassLoader = classLoader
            }
            try {
                method.invoke(null) as String
            } finally {
                if (savedClassLoader !== classLoader) {
                    Thread.currentThread().contextClassLoader = savedClassLoader
                }
            }
        }
        if (unexpectedBehaviour) {
            TestCase.assertNotSame("OK", result)
        } else {
            assertions.assertEquals("OK", result)
        }
    }

    private fun createAndVerifyClassLoader(
        module: TestModule,
        classFileFactory: ClassFileFactory,
        reportProblems: Boolean
    ): GeneratedClassLoader {
        val classLoader = createClassLoader(module, classFileFactory)
        val verificationSucceeded = CodegenTestUtil.verifyAllFilesWithAsm(classFileFactory, classLoader, reportProblems)
        if (!verificationSucceeded ) {
            assertions.fail { "Verification failed: see exceptions above" }
        }
        return classLoader
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun createClassLoader(module: TestModule, classFileFactory: ClassFileFactory): GeneratedClassLoader {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val urls = buildList {
            addAll(configuration.jvmClasspathRoots)
            testServices.compiledClassesManager.getCompiledJavaDirForModule(module)?.let {
                add(it)
            }
        }.map { it.toURI().toURL() }
        val parentClassLoader = if (configuration[TEST_CONFIGURATION_KIND_KEY]?.withReflection == true) {
            ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
        } else {
            ForTestCompileRuntime.runtimeJarClassLoader()
        }
        return GeneratedClassLoader(classFileFactory, parentClassLoader, *urls.toTypedArray())
    }

    private fun KtFile.getFacadeFqName(): String? {
        return runIf(getMemberDeclarationsToGenerate(this).isNotEmpty()) {
            getFileClassInfoNoResolve(this).facadeClassFqName.asString()
        }
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
