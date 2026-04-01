/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.KOTLIN_REFLECT_DUMP_MISMATCH
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_JAVA_FACADE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.lang.ref.SoftReference
import java.net.URL
import java.net.URLClassLoader
import kotlin.jvm.internal.Reflection
import kotlin.reflect.*
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.full.staticProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmName

/**
 * Dump testData declarations by using K1 kotlin-reflect, new kotlin-reflect implementation; and compare the dumps
 */
class JvmNewKotlinReflectCompatibilityCheck(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    private val k1ReflectStringBuilder = StringBuilder()
    private val newReflectStringBuilder = StringBuilder()
    private var skipAsserts = false

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        // Running the test is impossible if there are errors in Java code
        if (DISABLE_JAVA_FACADE in module.directives) return
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

        val fqNames = classPathFiles.flatMap { root ->
            root.walk()
                .filter { it.isFile && it.extension == "class" }
                .map { it.relativeTo(root).path.replace(File.separator, ".").removeSuffix(".class") }
        }.sorted()
        val k1ReflectDumpResult = runCatching { k1ReflectDumper.dumpKClasses(k1ReflectClassLoader, fqNames) }
        val newReflectDumpResult = runCatching { newReflectDumper.dumpKClasses(newReflectClassLoader, fqNames) }
        val exceptionK1Reflect = k1ReflectDumpResult.exceptionOrNull()
            ?.let { RuntimeException("Exception during K1 kotlin-reflect dumping", it) }
        val exceptionNewReflect = newReflectDumpResult.exceptionOrNull()
            ?.let { RuntimeException("Exception during New kotlin-reflect dumping", it) }
        when {
            exceptionK1Reflect == null && exceptionNewReflect == null -> {
                assertions.assertTrue(SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK !in module.directives) {
                    "Please drop SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK. kotlin-reflect didn't throw any exceptions"
                }
                val k1ReflectDump = k1ReflectDumpResult.getOrNull()!!
                val newReflectDump = newReflectDumpResult.getOrNull()!!
                k1ReflectStringBuilder.append(k1ReflectDump)
                newReflectStringBuilder.append(newReflectDump)
            }

            // An exception occurred
            SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK in module.directives -> skipAsserts = true
            else -> {
                val msg = when (exceptionK1Reflect != null && exceptionNewReflect != null) {
                    true -> "Exceptions during kotlin-reflect dumping in both implementations (K1 and New)\n"
                    else -> "One of the kotlin-reflects (K1 or New) failed, and another didn't\n"
                }
                assertions.fail {
                    listOfNotNull(
                        msg,
                        exceptionK1Reflect?.stackTraceToString()?.prependIndent()
                            ?.let { "K1 kotlin-reflect exception:\n$it" },
                        exceptionNewReflect?.stackTraceToString()?.prependIndent()
                            ?.let { "New kotlin-reflect exception:\n$it" }
                    ).joinToString("\n")
                }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val k1ReflectDump = k1ReflectStringBuilder.toString()
        val newReflectDump = newReflectStringBuilder.toString()
        val kotlinReflectDumpMismatch =
            KOTLIN_REFLECT_DUMP_MISMATCH in testServices.moduleStructure.allDirectives
        val k1ReflectFile =
            testServices.moduleStructure.originalTestDataFiles.first().withExtension(".reflect-k1.txt")
        val newReflectFile =
            testServices.moduleStructure.originalTestDataFiles.first().withExtension(".reflect-new.txt")
        if (kotlinReflectDumpMismatch) {
            assertions.assertFalse(skipAsserts) {
                "Cannot use both directives: " +
                        "${::SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK.name} and ${::KOTLIN_REFLECT_DUMP_MISMATCH.name}. " +
                        "Pick one"
            }
            val a = runCatching { assertions.assertEqualsToFile(k1ReflectFile, k1ReflectDump) }
            val b = runCatching { assertions.assertEqualsToFile(newReflectFile, newReflectDump) }
            a.getOrThrow()
            b.getOrThrow()

            assertions.assertTrue(k1ReflectDump != newReflectDump) {
                "K1 and new kotlin-reflect dumps are the same. Please drop ${::KOTLIN_REFLECT_DUMP_MISMATCH.name} directive"
            }
        } else {
            k1ReflectFile.delete()
            newReflectFile.delete()
            if (skipAsserts) return
            if (k1ReflectDump != newReflectDump) {
                val tip =
                    "// Tip: you can use ${::KOTLIN_REFLECT_DUMP_MISMATCH.name} or ${::SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK.name} " +
                            "directive to suppress the test\n"
                val k1ReflectHeader = "$tip// K1 kotlin-reflect dump\n"
                val newReflectHeader = "$tip// New kotlin-reflect dump\n"
                assertions.assertEquals(k1ReflectHeader + k1ReflectDump, newReflectHeader + newReflectDump)
            }
        }
    }

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
                .newInstanceInNewClassloader(testServices.standardLibrariesPathProvider.getRuntimeAndReflectWithNewFakeOverrridesJarClassLoader())
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
    private val method = alien::class.java.getMethod(dumpKClasses, *::dumpKClasses.javaMethod!!.parameterTypes)

    fun dumpKClasses(classLoader: ClassLoader, fqNames: List<String>): String =
        method.invoke(alien, classLoader, fqNames) as String
}

private const val dumpKClasses = "dumpKClasses"
private const val indentLiteral = "  "

// This class is run inside an ad-hoc classloader so that we can work with KClass conveniently statically
class RunInAlienClassLoader {
    @RequiresOptIn
    annotation class PrivateVisibility

    @PrivateVisibility // Emulate private visibility
    @JvmName(dumpKClasses)
    // The function name doesn't matter in compile time since it's always called with reflection
    fun `_`(loader: ClassLoader, fqNames: List<String>): String {
        val out = IndentedStringBuilder()
        for (fqn in fqNames) {
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
        // Listing some class statuses and superclasses makes it easier to read dumps
        val superclasses = kClass.superclasses.joinToString { it.simpleName.orEmpty() }
        val statuses = listOfNotNull(
            "abstract".takeIf { kClass.isAbstract },
            "sealed".takeIf { kClass.isSealed },
            if (kClass.java.isInterface) "interface" else "class",
        ).joinToString(separator = " ")
        indented("KClass: $statuses ${kClass.qualifiedName ?: kClass.jvmName} : $superclasses") {
            indented("members:") {
                val kCallables = kClass.members
                dumpKCallables(kCallables)
            }
            indented("declaredMembers:") {
                dumpKCallables(kClass.declaredMembers)
            }
            if (kClass.staticProperties.isNotEmpty() || kClass.staticFunctions.isNotEmpty()) {
                indented("staticMembers:") {
                    dumpKCallables(kClass.staticProperties)
                    dumpKCallables(kClass.staticFunctions)
                }
            }
        }
    }

    private fun IndentedStringBuilder.dumpKDeclarationContainer(container: KDeclarationContainer) {
        check(container !is KClass<*>)
        indented("KPackage: ${(container as kotlin.jvm.internal.ClassBasedDeclarationContainer).jClass.name}") {
            dumpKCallables(container.members)
        }
    }

    private fun IndentedStringBuilder.dumpKCallables(kCallables: Iterable<KCallable<*>>) {
        kCallables.map { kCallable ->
            val str = kCallable.toString()
            str to buildString {
                kCallable.visibility?.let { append("${it.toString().lowercase()} ") }
                if (kCallable.isOpen) append("open ")
                if (kCallable.isAbstract) append("abstract ")
                if (kCallable.isFinal) append("final ")
                if (kCallable.isSuspend) append("suspend ")
                if (kCallable is KFunction) {
                    if (kCallable.isOperator) append("operator ")
                    if (kCallable.isInfix) append("infix ")
                    if (kCallable.isInline) append("inline ")
                    if (kCallable.isExternal) append("external ")
                }
                val typeParameters = kCallable.typeParameters
                if (typeParameters.isNotEmpty()) {
                    append(typeParameters.joinToString(prefix = "<", postfix = "> ") { typeParameter ->
                        buildString {
                            if (typeParameter.isReified) append("reified ")
                            append(typeParameter)
                        }
                    })
                }

                append(str)

                val bounds = typeParameters.flatMap { typeParameter ->
                    typeParameter.upperBounds
                        .filter { it != typeOf<Any?>() }
                        .map { bound -> "${typeParameter.name} : $bound" }
                }
                if (bounds.isNotEmpty()) {
                    append(" where " + bounds.joinToString())
                }
            }
        }.sortedWith(compareBy(/* Sorting by it.first helps with readability when dumps mismatch */ { it.first }, { it.second }))
            .forEach { dump(it.second) }
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
