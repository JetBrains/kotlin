/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_NEW_KOTLIN_REFLECT_COMPAT_CHECK
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_JAVA_FACADE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.lang.ref.SoftReference
import java.net.URL
import java.net.URLClassLoader
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KDeclarationContainer
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.jvmName

/**
 * Dump testData declarations by using K1 kotlin-reflect, new kotlin-reflect implementation; and compare the dumps
 */
class JvmNewKotlinReflectCompatibilityCheck(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (DISABLE_JAVA_FACADE in module.directives) return
        if (SKIP_NEW_KOTLIN_REFLECT_COMPAT_CHECK in module.directives) return
        when (module.directives.singleOrZeroValue(JvmEnvironmentConfigurationDirectives.JDK_KIND)) {
            TestJdkKind.MOCK_JDK, TestJdkKind.MODIFIED_MOCK_JDK, TestJdkKind.FULL_JDK, null -> {}
            // Classes for newer JDK can't be loaded into the current old Java runtime (Java 8)
            TestJdkKind.FULL_JDK_11, TestJdkKind.FULL_JDK_17, TestJdkKind.FULL_JDK_21 -> return
        }
        val classPathFiles = computeTestRuntimeClasspath(testServices, module)

        val k1ReflectDumper = getK1KotlinReflectDumper(testServices)
        val newReflectDumper = getNewKotlinReflectDumper(testServices)

        val classPathUrls = classPathFiles.map { it.toURI().toURL() }.toTypedArray<URL>()
        val k1ReflectClassLoader = URLClassLoader(classPathUrls, k1ReflectDumper.classLoader)
        val newReflectClassLoader = URLClassLoader(classPathUrls, newReflectDumper.classLoader)

        val fqns = classPathFiles.flatMap { root ->
            root.walk()
                .filter { it.isFile && it.extension == "class" }
                .map { it.relativeTo(root).path.replace(File.separator, ".").removeSuffix(".class") }
        }
        val k1ReflectDumpResult = runCatching { k1ReflectDumper.dumpKClasses(k1ReflectClassLoader, fqns) }
        val newReflectDumpResult = runCatching { newReflectDumper.dumpKClasses(newReflectClassLoader, fqns) }
        val exceptionK1Reflect = k1ReflectDumpResult.exceptionOrNull()
            ?.let { RuntimeException("Exception during K1 kotlin-reflect dumping", it) }
        val exceptionNewReflect = newReflectDumpResult.exceptionOrNull()
            ?.let { RuntimeException("Exception during New kotlin-reflect dumping", it) }
        if (exceptionK1Reflect != null || exceptionNewReflect != null) {
            val msg = when (exceptionK1Reflect != null && exceptionNewReflect != null) {
                true -> "Exception during kotlin-reflect dumping in both implementations (K1 and New)"
                else -> "One of the kotlin-reflects (K1 or New) failed, and another didn't"
            }
            assertions.failAll(listOfNotNull(exceptionK1Reflect, exceptionNewReflect), msg)
        } else {
            val k1ReflectDump = k1ReflectDumpResult.getOrNull()!!
            val newReflectDump = newReflectDumpResult.getOrNull()!!
            if (k1ReflectDump != newReflectDump) {
                val k1ReflectHeader = "// K1 kotlin-reflect dump\n"
                val newReflectHeader = "// New kotlin-reflect dump\n"
                assertions.assertEquals(k1ReflectHeader + k1ReflectDump, newReflectHeader + newReflectDump)
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
        private var newKotlinReflectDumper: SoftReference<AlienInstance?> = SoftReference(null)
        private fun getNewKotlinReflectDumper(testServices: TestServices): AlienInstance {
            newKotlinReflectDumper.get()?.let { return it }
            return RunInAlienClassLoader::class.java
                .newInstanceInNewClassloader(testServices.standardLibrariesPathProvider.getRuntimeAndReflectJarClassLoader())
                .also { newKotlinReflectDumper = SoftReference(it) }
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
            val jClass = loader.loadClass(fqn)
            val metadata = jClass.annotations.firstIsInstanceOrNull<Metadata>()
            when (metadata?.kind) { // See kotlin.Metadata.kind for numbers meanings
                null, 1 -> out.dumpKClass(jClass.kotlin) // Kotlin and Java classes
                2 -> out.dumpKDeclarationContainer(Reflection.getOrCreateKotlinPackage(jClass)) // Facade file
                3 -> {} // Synthetic class
                4 -> out.dumpKDeclarationContainer(Reflection.getOrCreateKotlinPackage(jClass)) // Multi-file class facade
                5 -> out.dumpKDeclarationContainer(Reflection.getOrCreateKotlinPackage(jClass)) // Multi-file class part
                else -> error("Unknown kotlin.Metadata kind ${metadata.kind}")
            }
        }
        return out.toString()
    }

    private fun IndentedStringBuilder.dumpKClass(kClass: KClass<*>) {
        indented("KClass: ${kClass.qualifiedName ?: kClass.jvmName}") {
            indented("members:") {
                dumpKCallables(kClass.members)
            }
            indented("declaredMembers:") {
                dumpKCallables(kClass.declaredMembers)
            }
        }
    }

    private fun IndentedStringBuilder.dumpKDeclarationContainer(container: KDeclarationContainer) {
        check(container !is KClass<*>)
        indented("KDeclarationContainer: ${container::class.qualifiedName ?: container::class.jvmName}") {
            dumpKCallables(container.members)
        }
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
                out.append(indentation)
                out.appendLine(line)
            }
        }

        override fun toString(): String = out.toString()
    }
}
