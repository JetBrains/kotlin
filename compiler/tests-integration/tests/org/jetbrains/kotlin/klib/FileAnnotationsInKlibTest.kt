/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
import java.nio.file.Path

/**
 * Tests that `@file:` annotations are serialized into KLib metadata
 * and can be read back from the `file_annotation` extension field in `PackageFragment`.
 */
class FileAnnotationsInKlibTest : KtUsefulTestCase() {
    companion object {
        private const val MODULE_NAME = "testLib"
    }

    private val runtimeKlibPath = "libraries/stdlib/build/classes/kotlin/js/main"

    private fun compileJsKlib(sourceText: String, workingDir: File): File {
        val sourceFile = File(workingDir, "source.kt")
        sourceFile.writeText(sourceText)

        val artifact = File(workingDir, "$MODULE_NAME.klib")
        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JSCompiler(),
            listOf(
                sourceFile.absolutePath,
                "-Xir-produce-klib-file",
                "-ir-output-dir", artifact.parentFile.absolutePath,
                "-ir-output-name", MODULE_NAME,
                "-libraries", runtimeKlibPath,
            )
        )

        return artifact
    }

    private fun loadFileAnnotations(klibFile: File): Map<String, List<String>> {
        val libs = KlibLoader {
            libraryPaths(listOf(runtimeKlibPath, klibFile.canonicalPath))
            platformChecker(KlibPlatformChecker.JS)
        }.load().apply { assertFalse(hasProblems) }.librariesStdlibFirst

        val lib = libs.last()
        val metadata = lib.metadata
        val headerProto = parseModuleHeader(metadata.moduleHeaderData)

        val result = mutableMapOf<String, List<String>>()

        for (packageFragmentName in headerProto.packageFragmentNameList) {
            for (packageMetadataPart in metadata.getPackageFragmentNames(packageFragmentName)) {
                val packageMetadata = metadata.getPackageFragment(packageFragmentName, packageMetadataPart)
                val fragment = parsePackageFragment(packageMetadata)
                val nameResolver = NameResolverImpl(fragment.strings, fragment.qualifiedNames)

                val fileAnnotations = fragment.getExtension(KlibMetadataProtoBuf.fileAnnotation)
                if (fileAnnotations.isNullOrEmpty()) continue

                val annotationNames = fileAnnotations.map { annotation ->
                    nameResolver.getClassId(annotation.id).asSingleFqName().asString()
                }

                result[packageFragmentName] = annotationNames
            }
        }

        return result
    }

    fun testFileAnnotationsAreSerialized() {
        withTempDir { dir ->
            val klibFile = compileJsKlib(
                """
                @file:Suppress("UNUSED_PARAMETER")
                @file:OptIn(ExperimentalStdlibApi::class)
                
                package test
                
                fun foo(x: Int) = 42
                """.trimIndent(),
                dir,
            )

            val annotations = loadFileAnnotations(klibFile)
            val testAnnotations = annotations["test"]
            assertNotNull("Expected file annotations for package 'test'", testAnnotations)
            assertTrue(
                "Expected kotlin.Suppress in file annotations, got: $testAnnotations",
                testAnnotations!!.any { it == "kotlin.Suppress" },
            )
            assertTrue(
                "Expected kotlin.OptIn in file annotations, got: $testAnnotations",
                testAnnotations.any { it == "kotlin.OptIn" },
            )
        }
    }

    fun testCustomFileAnnotation() {
        withTempDir { dir ->
            val klibFile = compileJsKlib(
                """
                @file:MyFileAnnotation("hello")
                
                package test
                
                @Target(AnnotationTarget.FILE)
                @Retention(AnnotationRetention.BINARY)
                annotation class MyFileAnnotation(val value: String)
                
                fun bar() = "bar"
                """.trimIndent(),
                dir,
            )

            val annotations = loadFileAnnotations(klibFile)
            val testAnnotations = annotations["test"]
            assertNotNull("Expected file annotations for package 'test'", testAnnotations)
            assertTrue(
                "Expected test.MyFileAnnotation in file annotations, got: $testAnnotations",
                testAnnotations!!.any { it == "test.MyFileAnnotation" },
            )
        }
    }

    fun testNoFileAnnotations() {
        withTempDir { dir ->
            val klibFile = compileJsKlib(
                """
                package test
                
                fun baz() = 0
                """.trimIndent(),
                dir,
            )

            val annotations = loadFileAnnotations(klibFile)
            val testAnnotations = annotations["test"]
            assertTrue(
                "Expected no file annotations for package 'test', got: $testAnnotations",
                testAnnotations.isNullOrEmpty(),
            )
        }
    }

    private fun withTempDir(f: (File) -> Unit) {
        val workingPath: Path = kotlin.io.path.createTempDirectory()
        val workingDirFile = workingPath.toFile().also { assert(it.isDirectory) }
        try {
            f(workingDirFile)
        } finally {
            workingDirFile.deleteRecursively()
        }
    }
}
