/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.java.direct.util.DefaultJavaSourceFileReader
import org.jetbrains.kotlin.java.direct.util.extractFileInfoLightweight
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class JavaParsingLightweightScannerTest : JavaParsingTestBase() {

    @Test
    fun testLightweightScannerBasic(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Foo.java")
        file.writeText(
            """
            package com.example;

            public class Foo {
                int x;
            }
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info != null) { "Expected non-null LightweightFileInfo" }
        assert(info!!.packageName == "com.example") { "Expected package 'com.example', got '${info.packageName}'" }
        assert(info.topLevelClassNames == setOf("Foo")) { "Expected {Foo}, got ${info.topLevelClassNames}" }
    }

    @Test
    fun testLightweightScannerPackageWithoutTrailingSemicolon(@TempDir tempDir: Path) {
        // Several Kotlin diagnostic test-data files (e.g. `EnumEntryVsStaticAmbiguity4.kt`,
        // `kt57845.kt`) declare `package foo` without a trailing `;` and rely on PSI's
        // error-tolerant Java parser. PACKAGE_REGEX must accept both forms so that source-side
        // resolution stays consistent with PSI once `BinaryJavaClassFinder` is the binary half.
        val file = tempDir.resolve("Foo.java")
        file.writeText(
            """
            package com.example

            public class Foo {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info != null) { "Expected non-null LightweightFileInfo" }
        assert(info!!.packageName == "com.example") { "Expected package 'com.example', got '${info.packageName}'" }
        assert(info.topLevelClassNames == setOf("Foo")) { "Expected {Foo}, got ${info.topLevelClassNames}" }
    }

    @Test
    fun testLightweightScannerDefaultPackage(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Bar.java")
        file.writeText(
            """
            public class Bar {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info != null) { "Expected non-null LightweightFileInfo" }
        assert(info!!.packageName == null) { "Expected null package (default), got '${info.packageName}'" }
        assert(info.topLevelClassNames == setOf("Bar")) { "Expected {Bar}, got ${info.topLevelClassNames}" }
    }

    @Test
    fun testLightweightScannerMultipleClasses(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Multi.java")
        file.writeText(
            """
            package test;

            public class Multi {}
            class Helper {}
            interface Service {}
            enum Color { RED, GREEN, BLUE }
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info != null) { "Expected non-null LightweightFileInfo" }
        assert(info!!.packageName == "test") { "Expected package 'test', got '${info.packageName}'" }
        assert(info.topLevelClassNames == setOf("Multi", "Helper", "Service", "Color")) {
            "Expected {Multi, Helper, Service, Color}, got ${info.topLevelClassNames}"
        }
    }

    @Test
    fun testLightweightScannerIgnoresComments(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Comments.java")
        file.writeText(
            """
            package test;

            // class NotAClass {}
            /* class AlsoNotAClass {} */
            /**
             * class StillNotAClass {}
             * This is a Javadoc comment.
             */
            public class Comments {
                // class InnerNotAClass {}
            }
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info != null) { "Expected non-null LightweightFileInfo" }
        assert(info!!.topLevelClassNames == setOf("Comments")) {
            "Expected only {Comments}, got ${info.topLevelClassNames}"
        }
    }

    @Test
    fun testLightweightScannerIgnoresNestedClasses(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Outer.java")
        file.writeText(
            """
            package test;

            public class Outer {
                public class Inner {}
                static class StaticNested {}
                interface NestedIface {}
            }
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info != null) { "Expected non-null LightweightFileInfo" }
        assert(info!!.topLevelClassNames == setOf("Outer")) {
            "Expected only {Outer}, got ${info.topLevelClassNames}"
        }
    }

    @Test
    fun testLightweightScannerBlockCommentAcrossLines(@TempDir tempDir: Path) {
        val file = tempDir.resolve("BlockComment.java")
        file.writeText(
            """
            package test;

            /*
            class Hidden {
            }
            */
            public class BlockComment {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info != null) { "Expected non-null LightweightFileInfo" }
        assert(info!!.topLevelClassNames == setOf("BlockComment")) {
            "Expected only {BlockComment}, got ${info.topLevelClassNames}"
        }
    }

    @Test
    fun testLightweightScannerRecordDeclaration(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Point.java")
        file.writeText(
            """
            package geometry;

            public record Point(int x, int y) {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info != null) { "Expected non-null LightweightFileInfo" }
        assert(info!!.packageName == "geometry") { "Expected package 'geometry', got '${info.packageName}'" }
        assert(info.topLevelClassNames == setOf("Point")) { "Expected {Point}, got ${info.topLevelClassNames}" }
    }

    @Test
    fun testLightweightScannerNoClasses(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Empty.java")
        file.writeText(
            """
            package test;
            // Just a file with no classes
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info == null) { "Expected null for file with no class declarations" }
    }

    @Test
    fun testLightweightScannerAnnotationType(@TempDir tempDir: Path) {
        val file = tempDir.resolve("MyAnnotation.java")
        file.writeText(
            """
            package annotations;

            public @interface MyAnnotation {
                String value() default "";
            }
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toVirtualFile(), DefaultJavaSourceFileReader)
        assert(info != null) { "Expected non-null LightweightFileInfo" }
        assert(info!!.packageName == "annotations") { "Expected 'annotations', got '${info.packageName}'" }
        // @interface declares a type named MyAnnotation — the scanner extracts "MyAnnotation" from "interface MyAnnotation"
        assert("MyAnnotation" in info.topLevelClassNames) {
            "Expected MyAnnotation in class names, got ${info.topLevelClassNames}"
        }
    }

    @Test
    fun testSmallFileCachedDuringIndexing(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("com/example")
        pkgDir.toFile().mkdirs()
        pkgDir.resolve("Small.java").writeText(
            """
            package com.example;
            public class Small {
                public int field;
            }
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // findClass should succeed (class was cached during indexing for small files)
        val classId = ClassId(FqName("com.example"), Name.identifier("Small"))
        val request = JavaClassFinder.Request(classId)
        val javaClass = finder.findClass(request)

        assert(javaClass != null) { "Expected to find Small class" }
        assert(javaClass?.name?.asString() == "Small") { "Expected name 'Small'" }
        assert(javaClass?.fields?.size == 1) { "Expected 1 field, got ${javaClass?.fields?.size}" }
    }

    @Test
    fun testSmallFileMultipleClassesCachedTogether(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("test")
        pkgDir.toFile().mkdirs()
        // Small file with two top-level classes
        pkgDir.resolve("Main.java").writeText(
            """
            package test;
            public class Main {}
            class Helper {}
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // Both classes should be findable after a single parse during indexing
        val mainId = ClassId(FqName("test"), Name.identifier("Main"))
        val helperId = ClassId(FqName("test"), Name.identifier("Helper"))

        val mainClass = finder.findClass(JavaClassFinder.Request(mainId))
        val helperClass = finder.findClass(JavaClassFinder.Request(helperId))

        assert(mainClass != null) { "Expected to find Main class" }
        assert(helperClass != null) { "Expected to find Helper class" }
        assert(mainClass?.name?.asString() == "Main")
        assert(helperClass?.name?.asString() == "Helper")
    }

    @Test
    fun testLargeFileLightweightIndexing(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("com/big")
        pkgDir.toFile().mkdirs()

        // Create a file larger than SMALL_FILE_SIZE_THRESHOLD (4096 bytes)
        val sb = StringBuilder()
        sb.appendLine("package com.big;")
        sb.appendLine()
        sb.appendLine("public class Large {")
        // Add enough fields to exceed 4KB
        for (i in 1..200) {
            sb.appendLine("    public int field$i;")
        }
        sb.appendLine("}")
        val largeContent = sb.toString()
        assert(largeContent.toByteArray().size > 4096) { "Test file should be > 4KB" }

        pkgDir.resolve("Large.java").writeText(largeContent)

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // Class should be indexed and findable despite using lightweight scanning
        val classId = ClassId(FqName("com.big"), Name.identifier("Large"))
        val request = JavaClassFinder.Request(classId)
        val javaClass = finder.findClass(request)

        assert(javaClass != null) { "Expected to find Large class" }
        assert(javaClass?.name?.asString() == "Large") { "Expected name 'Large'" }
        assert(javaClass?.fields?.size == 200) { "Expected 200 fields, got ${javaClass?.fields?.size}" }
    }

    @Test
    fun testLargeFileSiblingClassesCachedTogether(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("test")
        pkgDir.toFile().mkdirs()

        // Large file with two top-level classes
        val sb = StringBuilder()
        sb.appendLine("package test;")
        sb.appendLine("public class BigMain {")
        for (i in 1..200) {
            sb.appendLine("    public int field$i;")
        }
        sb.appendLine("}")
        sb.appendLine("class BigHelper {}")
        val largeContent = sb.toString()
        assert(largeContent.toByteArray().size > 4096) { "Test file should be > 4KB" }

        pkgDir.resolve("BigMain.java").writeText(largeContent)

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // First access triggers full parse, which should cache both classes
        val mainId = ClassId(FqName("test"), Name.identifier("BigMain"))
        val helperId = ClassId(FqName("test"), Name.identifier("BigHelper"))

        val mainClass = finder.findClass(JavaClassFinder.Request(mainId))
        assert(mainClass != null) { "Expected to find BigMain class" }

        // BigHelper should also be cached from the same parse (no additional file I/O)
        val helperClass = finder.findClass(JavaClassFinder.Request(helperId))
        assert(helperClass != null) { "Expected to find BigHelper class" }
    }
}
