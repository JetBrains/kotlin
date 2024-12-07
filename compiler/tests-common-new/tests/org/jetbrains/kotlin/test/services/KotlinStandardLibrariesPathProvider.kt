/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.configureStandardLibs
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_FULL_STDLIB_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_JS_KOTLIN_TEST_KLIB_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_JS_REDUCED_STDLIB_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_JS_STDLIB_KLIB_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_MINIMAL_STDLIB_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_REFLECT_JAR_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_SCRIPT_RUNTIME_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_TEST_JAR_PATH
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.lang.ref.SoftReference
import java.net.URL
import java.net.URLClassLoader

abstract class KotlinStandardLibrariesPathProvider : TestService {
    companion object {
        @Volatile
        private var runtimeJarClassLoader: SoftReference<ClassLoader?> = SoftReference(null)

        @Volatile
        private var reflectJarClassLoader: SoftReference<ClassLoader?> = SoftReference(null)

        private fun createClassLoader(vararg files: File): ClassLoader {
            val urls: MutableList<URL> = ArrayList(2)
            for (file in files) {
                urls.add(file.toURI().toURL())
            }
            return URLClassLoader(urls.toTypedArray(), null)
        }
    }

    /**
     * kotlin-stdlib.jar
     */
    abstract fun runtimeJarForTests(): File

    /**
     * kotlin-stdlib-jdk8.jar
     */
    abstract fun runtimeJarForTestsWithJdk8(): File

    /**
     * Jar with minimal version of kotlin stdlib (may be same as runtimeJarForTests)
     */
    abstract fun minimalRuntimeJarForTests(): File

    /**
     * kotlin-reflect.jar
     */
    abstract fun reflectJarForTests(): File

    /**
     * kotlin-test.jar
     */
    abstract fun kotlinTestJarForTests(): File

    /**
     * kotlin-script-runtime.jar
     */
    abstract fun scriptRuntimeJarForTests(): File

    /**
     * kotlin-annotations-jvm.jar
     */
    abstract fun jvmAnnotationsForTests(): File

    /**
     * compiler/testData/mockJDK/jre/lib/annotations.jar
     */
    abstract fun getAnnotationsJar(): File

    /**
     * kotlin-stdlib-js.klib
     */
    abstract fun fullJsStdlib(): File

    /**
     * Jar with minimal version of kotlin stdlib JS (may be same as fullJsStdlib)
     */
    abstract fun defaultJsStdlib(): File

    /**
     * kotlin-test-js.jar
     */
    abstract fun kotlinTestJsKLib(): File

    /**
     * scriptingPlugin classpath jars
     */
    abstract fun scriptingPluginFilesForTests(): Collection<File>

    fun getRuntimeJarClassLoader(): ClassLoader = synchronized(this) {
        var loader = runtimeJarClassLoader.get()
        if (loader == null) {
            loader = createClassLoader(
                runtimeJarForTests(),
                scriptRuntimeJarForTests(),
                kotlinTestJarForTests()
            )
            runtimeJarClassLoader = SoftReference(loader)
        }
        loader
    }

    fun getRuntimeAndReflectJarClassLoader(): ClassLoader = synchronized(this) {
        var loader = reflectJarClassLoader.get()
        if (loader == null) {
            loader = createClassLoader(
                runtimeJarForTests(),
                reflectJarForTests(),
                scriptRuntimeJarForTests(),
                kotlinTestJarForTests()
            )
            reflectJarClassLoader = SoftReference(loader)
        }
        loader
    }
}

object StandardLibrariesPathProviderForKotlinProject : KotlinStandardLibrariesPathProvider() {
    override fun runtimeJarForTests(): File =
        extractFromPropertyFirstFile(KOTLIN_FULL_STDLIB_PATH) { ForTestCompileRuntime.runtimeJarForTests() }

    override fun runtimeJarForTestsWithJdk8(): File = ForTestCompileRuntime.runtimeJarForTestsWithJdk8()
    override fun minimalRuntimeJarForTests(): File =
        extractFromPropertyFirstFile(KOTLIN_MINIMAL_STDLIB_PATH) { ForTestCompileRuntime.minimalRuntimeJarForTests() }

    override fun reflectJarForTests(): File =
        extractFromPropertyFirstFile(KOTLIN_REFLECT_JAR_PATH) { ForTestCompileRuntime.reflectJarForTests() }

    override fun kotlinTestJarForTests(): File =
        extractFromPropertyFirstFile(KOTLIN_TEST_JAR_PATH) { ForTestCompileRuntime.kotlinTestJarForTests() }

    override fun scriptRuntimeJarForTests(): File =
        extractFromPropertyFirstFile(KOTLIN_SCRIPT_RUNTIME_PATH) { ForTestCompileRuntime.scriptRuntimeJarForTests() }

    override fun jvmAnnotationsForTests(): File = ForTestCompileRuntime.jvmAnnotationsForTests()
    override fun getAnnotationsJar(): File =
        KtTestUtil.getAnnotationsJar().also {
            assert(it.exists()) { "AnnotationJar missing: $it does not exist" }
        }

    override fun fullJsStdlib(): File = extractFromPropertyFirst(KOTLIN_JS_STDLIB_KLIB_PATH) { "kotlin-stdlib-js.klib".dist() }
    override fun defaultJsStdlib(): File = extractFromPropertyFirst(KOTLIN_JS_REDUCED_STDLIB_PATH) { "kotlin-stdlib-js.klib".dist() }
    override fun kotlinTestJsKLib(): File = extractFromPropertyFirst(KOTLIN_JS_KOTLIN_TEST_KLIB_PATH) { "kotlin-test-js.klib".dist() }
    override fun scriptingPluginFilesForTests(): Collection<File> =
        extractFromPropertyFirstFiles(KOTLIN_SCRIPTING_PLUGIN_CLASSPATH) {
            val libPath = PathUtil.kotlinPathsForCompiler.libPath
            val pluginClasspath = with(PathUtil) {
                listOf(
                    KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR,
                    KOTLIN_SCRIPTING_COMPILER_IMPL_JAR,
                    KOTLIN_SCRIPTING_COMMON_JAR,
                    KOTLIN_SCRIPTING_JVM_JAR
                ).map {
                    val file = File(libPath, it)
                    if (!file.exists()) {
                        throw Error("Missing ${file.path}")
                    }
                    file
                }
            }
            pluginClasspath
        }

    private inline fun extractFromPropertyFirst(prop: String, onMissingProperty: () -> String): File {
        val path = System.getProperty(prop, null) ?: onMissingProperty()
        assert(File(path).exists()) { "$path not found" }
        return File(path)
    }

    private inline fun extractFromPropertyFirstFile(prop: String, onMissingProperty: () -> File): File {
        return System.getProperty(prop, null)?.let {
            val f = File(it)
            assert(f.exists()) { "$it not found" }
            f
        } ?: onMissingProperty()
    }

    private inline fun extractFromPropertyFirstFiles(prop: String, onMissingProperty: () -> Collection<File>): Collection<File> {
        return System.getProperty(prop, null)?.split(",")?.map {
            val f = File(it)
            assert(f.exists()) { "$it not found" }
            f
        } ?: onMissingProperty()
    }

    private fun String.dist(): String {
        return "dist/kotlinc/lib/$this"
    }
}

object EnvironmentBasedStandardLibrariesPathProvider : KotlinStandardLibrariesPathProvider() {
    const val KOTLIN_STDLIB_PROP = "org.jetbrains.kotlin.test.kotlin-stdlib"
    const val KOTLIN_STDLIB_JS_PROP = "org.jetbrains.kotlin.test.kotlin-stdlib-js"
    const val KOTLIN_STDLIB_JDK8_PROP = "org.jetbrains.kotlin.test.kotlin-stdlib-jdk8"
    const val KOTLIN_REFLECT_PROP = "org.jetbrains.kotlin.test.kotlin-reflect"
    const val KOTLIN_TEST_PROP = "org.jetbrains.kotlin.test.kotlin-test"
    const val KOTLIN_TEST_JS_PROP = "org.jetbrains.kotlin.test.kotlin-test-js"
    const val KOTLIN_SCRIPT_RUNTIME_PROP = "org.jetbrains.kotlin.test.kotlin-script-runtime"
    const val KOTLIN_ANNOTATIONS_JVM_PROP = "org.jetbrains.kotlin.test.kotlin-annotations-jvm"

    fun getFile(propertyName: String): File {
        return System.getProperty(propertyName)
            ?.let(::File)
            ?.takeIf { it.exists() }
            ?: error("Property $propertyName is not set or file under it not found")
    }

    override fun runtimeJarForTests(): File = getFile(KOTLIN_STDLIB_PROP)
    override fun runtimeJarForTestsWithJdk8(): File = getFile(KOTLIN_STDLIB_JDK8_PROP)
    override fun minimalRuntimeJarForTests(): File = getFile(KOTLIN_STDLIB_PROP)
    override fun reflectJarForTests(): File = getFile(KOTLIN_REFLECT_PROP)
    override fun kotlinTestJarForTests(): File = getFile(KOTLIN_TEST_PROP)
    override fun scriptRuntimeJarForTests(): File = getFile(KOTLIN_SCRIPT_RUNTIME_PROP)
    override fun jvmAnnotationsForTests(): File = getFile(KOTLIN_ANNOTATIONS_JVM_PROP)
    override fun getAnnotationsJar(): File = getFile(KOTLIN_ANNOTATIONS_JVM_PROP)
    override fun fullJsStdlib(): File = getFile(KOTLIN_STDLIB_JS_PROP)
    override fun defaultJsStdlib(): File = getFile(KOTLIN_STDLIB_JS_PROP)
    override fun kotlinTestJsKLib(): File = getFile(KOTLIN_TEST_JS_PROP)
    override fun scriptingPluginFilesForTests(): Collection<File> {
        TODO("KT-67573")
    }
}

val TestServices.standardLibrariesPathProvider: KotlinStandardLibrariesPathProvider by TestServices.testServiceAccessor()

fun CompilerConfiguration.configureStandardLibs(
    pathProvider: KotlinStandardLibrariesPathProvider,
    arguments: K2JVMCompilerArguments
) {
    configureStandardLibs(
        pathProvider,
        KotlinStandardLibrariesPathProvider::runtimeJarForTests,
        KotlinStandardLibrariesPathProvider::scriptRuntimeJarForTests,
        KotlinStandardLibrariesPathProvider::reflectJarForTests,
        arguments
    )
}
