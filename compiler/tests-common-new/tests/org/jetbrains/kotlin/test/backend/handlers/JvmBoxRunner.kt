/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import junit.framework.TestCase
import org.jetbrains.kotlin.backend.common.CodegenUtil.getMemberDeclarationsToGenerate
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil.getFileClassInfoNoResolve
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.lang.reflect.Method
import java.net.URLClassLoader

class JvmBoxRunner(
    testServices: TestServices
) : JvmBinaryArtifactHandler(testServices) {
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
        val classLoader = createClassLoader(module, info.classFileFactory)
        for (ktFile in ktFiles) {
            val className = ktFile.getFacadeFqName() ?: continue
            val clazz = classLoader.getGeneratedClass(className)
            val method = clazz.getBoxMethodOrNull() ?: continue
            boxMethodFound = true
            callBoxMethodAndCheckResult(classLoader, clazz, method, unexpectedBehaviour = false)
            return
        }
    }

    private fun callBoxMethodAndCheckResult(
        classLoader: URLClassLoader,
        clazz: Class<*>?,
        method: Method,
        unexpectedBehaviour: Boolean
    ) {
        val result = if (BOX_IN_SEPARATE_PROCESS_PORT != null) {
            TODO()
//            result = invokeBoxInSeparateProcess(classLoader, aClass)
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

    private fun createClassLoader(module: TestModule, classFileFactory: ClassFileFactory): GeneratedClassLoader {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val urls = configuration.jvmClasspathRoots.map { it.toURI().toURL() }
        val classLoader = URLClassLoader(urls.toTypedArray())
        return GeneratedClassLoader(classFileFactory, classLoader)
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
