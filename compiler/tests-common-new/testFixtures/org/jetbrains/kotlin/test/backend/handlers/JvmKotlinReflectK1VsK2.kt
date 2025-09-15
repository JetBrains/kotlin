/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import java.io.File
import java.lang.ref.SoftReference
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KCallable
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.jvmName

class JvmKotlinReflectK1VsK2(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        when (module.directives.singleOrZeroValue(JvmEnvironmentConfigurationDirectives.JDK_KIND)) {
            TestJdkKind.MOCK_JDK, TestJdkKind.MODIFIED_MOCK_JDK, TestJdkKind.FULL_JDK, null -> {}
            // Classes for newer JDK can't be loaded into the current old Java runtime (Java 8)
            TestJdkKind.FULL_JDK_11, TestJdkKind.FULL_JDK_17, TestJdkKind.FULL_JDK_21 -> return
        }
        val classPathFiles = computeTestRuntimeClasspath(testServices, module)

        val k1Dumper = getK1KotlinReflectDumper(testServices)
        val k2Dumper = getK2KotlinReflectDumper(testServices)

        val classPathUrls = classPathFiles.map { it.toURI().toURL() }.toTypedArray<URL>()
        val k1ClassLoader = URLClassLoader(classPathUrls, k1Dumper.classLoader)
        val k2ClassLoader = URLClassLoader(classPathUrls, k2Dumper.classLoader)

        val fqns = classPathFiles.flatMap { root ->
            root.walk()
                .filter { it.isFile && it.extension == "class" }
                .map { it.relativeTo(root).path.replace(File.separator, ".").removeSuffix(".class") }
        }
        val k1DumpResult = runCatching { k1Dumper.dumpKClasses(k1ClassLoader, fqns) }
        val k2DumpResult = runCatching { k2Dumper.dumpKClasses(k2ClassLoader, fqns) }
        val exceptionK1 = k1DumpResult.exceptionOrNull()
            ?.let { RuntimeException("Exception during K1 kotlin-reflect dumping", it) }
        val exceptionK2 = k2DumpResult.exceptionOrNull()
            ?.let { RuntimeException("Exception during K2 kotlin-reflect dumping", it) }
        if (exceptionK1 != null || exceptionK2 != null) {
            val msg = when (exceptionK1 != null && exceptionK2 != null) {
                true -> "Exception during kotlin-reflect dumping in both implementations (K1 and K2)"
                else -> "One of the kotlin-reflects (K1 or K2) failed, and another didn't"
            }
            assertions.failAll(listOfNotNull(exceptionK1, exceptionK2), msg)
        } else {
            val k1Dump = k1DumpResult.getOrNull()!!
            val k2Dump = k2DumpResult.getOrNull()!!
            if (k1Dump != k2Dump) {
                val k1Header = "// K1 kotlin-reflect dump\n"
                val k2Header = "// K2 kotlin-reflect dump\n"
                assertions.assertEquals(k1Header + k1Dump, k2Header + k2Dump)
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    companion object {
        // Use SoftReference because it's the way classloaders in KotlinStandardLibrariesPathProvider are implemented.
        // This variable, unfortunately, keeps a reference to those classloaders
        private var k1KotlinReflectDumper: SoftReference<AlienInstance?> = SoftReference(null)
        private fun getK1KotlinReflectDumper(testServices: TestServices): AlienInstance {
            k1KotlinReflectDumper.get()?.let { return it }
            return RunInAlienClassLoader::class.java
                .newInstanceInNewClassloader(testServices.standardLibrariesPathProvider.getRuntimeAndK1ReflectJarClassLoader())
                .also { k1KotlinReflectDumper = SoftReference(it) }
        }

        // Use SoftReference because it's the way classloaders in KotlinStandardLibrariesPathProvider are implemented.
        // This variable, unfortunately, keeps a reference to those classloaders
        private var k2KotlinReflectDumper: SoftReference<AlienInstance?> = SoftReference(null)
        private fun getK2KotlinReflectDumper(testServices: TestServices): AlienInstance {
            k2KotlinReflectDumper.get()?.let { return it }
            return RunInAlienClassLoader::class.java
                .newInstanceInNewClassloader(testServices.standardLibrariesPathProvider.getRuntimeAndReflectJarClassLoader())
                .also { k2KotlinReflectDumper = SoftReference(it) }
        }
    }
}

private fun Class<*>.newInstanceInNewClassloader(parentClassLoader: ClassLoader?): AlienInstance {
    val classLoader = URLClassLoader(arrayOf(protectionDomain.codeSource.location), parentClassLoader)
    return AlienInstance(Class.forName(name, true, classLoader).newInstance())
}

class AlienInstance(private val alien: Any) {
    val classLoader: ClassLoader get() = alien.javaClass.classLoader
    fun dumpKClasses(classLoader: ClassLoader, fqns: List<String>): String =
        alien::class.java.getMethod(dumpKClasses, ClassLoader::class.java, List::class.java)
            .invoke(alien, classLoader, fqns) as String
}

private const val dumpKClasses = "dumpKClasses"
private const val indentLiteral = "  "

// This class is run inside an ad-hoc classloader so that we can work with KClass conveniently statically
class RunInAlienClassLoader {
    @Deprecated("Emulate private visibility", level = DeprecationLevel.ERROR)
    @JvmName(dumpKClasses)
    // The function name doesn't matter in compile time since it's always called with reflection
    fun `_`(loader: ClassLoader, fqns: List<String>): String {
        val out = IndentedStringBuilder()
        for (fqn in fqns) {
            val kClass = loader.loadClass(fqn).kotlin
            out.indented("KClass: ${kClass.qualifiedName ?: kClass.jvmName}") {
                out.indented("members:") {
                    out.dumpKCallables(kClass.members)
                }
                out.indented("declaredMembers:") {
                    out.dumpKCallables(kClass.declaredMembers)
                }
            }
        }
        return out.toString()
    }

    private fun IndentedStringBuilder.dumpKCallables(kCallables: Iterable<KCallable<*>>) {
        kCallables.map { it.toString() }.sorted().forEach { dump(it) }
    }

    private class IndentedStringBuilder {
        private val out = StringBuilder()
        private var indentation = ""

        inline fun <T> indented(header: String = "", body: () -> T): T {
            if (header != "") {
                dump(header)
            }
            val prev = indentation
            indentation += indentLiteral
            try {
                return body()
            } finally {
                indentation = prev
            }
        }

        fun dump(str: String) {
            for (line in str.split("\n")) {
                out.appendLine(indentation + line)
            }
        }

        override fun toString(): String = out.toString()
    }
}
