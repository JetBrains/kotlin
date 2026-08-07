/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.java.direct.util.extractFileInfoLightweight
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
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

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals("com.example", info.packageName)
        assertEquals(setOf("Foo"), info.topLevelClassNames)
    }

    @Test
    fun testLightweightScannerPackageWithoutTrailingSemicolon(@TempDir tempDir: Path) {
        // Some test-data files (e.g. KT-57845, EnumEntryVsStaticAmbiguity4.kt) omit the trailing
        // semicolon in package declarations. The scan accepts both forms for consistency with PSI.
        val file = tempDir.resolve("Foo.java")
        file.writeText(
            """
            package com.example

            public class Foo {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals("com.example", info.packageName)
        assertEquals(setOf("Foo"), info.topLevelClassNames)
    }

    @Test
    fun testLightweightScannerPackageWithCommentsBetweenSegments(@TempDir tempDir: Path) {
        // Comments and line breaks are allowed anywhere between the segments of a package name.
        val file = tempDir.resolve("Foo.java")
        file.writeText(
            """
            package builder // line comment
                . /* block comment */ subpackage;

            public class Foo {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals("builder.subpackage", info.packageName)
        assertEquals(setOf("Foo"), info.topLevelClassNames)
    }

    @Test
    fun testLightweightScannerMalformedPackageName(@TempDir tempDir: Path) {
        // `com.123` is not a valid package name: the scan stops at the malformed segment without
        // emitting a trailing dot, and still indexes the file's classes.
        val file = tempDir.resolve("Foo.java")
        file.writeText(
            """
            package com.123;

            public class Foo {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals("com", info.packageName)
        assertEquals(setOf("Foo"), info.topLevelClassNames)
    }

    @Test
    fun testLightweightScannerMalformedClassName(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Valid.java")
        file.writeText(
            """
            package test;

            class 456 {}
            class Valid {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals("test", info.packageName)
        assertEquals(setOf("Valid"), info.topLevelClassNames)
    }

    @Test
    fun testLightweightScannerOnlyMalformedClassName(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Broken.java")
        file.writeText(
            """
            package test;

            class 456 {}
        """.trimIndent()
        )

        assertNull(extractFileInfoLightweight(file.toFile()))
    }

    @Test
    fun testLightweightScannerDefaultPackage(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Bar.java")
        file.writeText(
            """
            public class Bar {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertNull(info.packageName)
        assertEquals(setOf("Bar"), info.topLevelClassNames)
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

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals("test", info.packageName)
        assertEquals(setOf("Multi", "Helper", "Service", "Color"), info.topLevelClassNames)
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

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals(setOf("Comments"), info.topLevelClassNames)
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

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals(setOf("Outer"), info.topLevelClassNames)
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

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals(setOf("BlockComment"), info.topLevelClassNames)
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

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals("geometry", info.packageName)
        assertEquals(setOf("Point"), info.topLevelClassNames)
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

        assertNull(extractFileInfoLightweight(file.toFile()))
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

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals("annotations", info.packageName)
        // @interface declares a type named MyAnnotation — the scanner extracts "MyAnnotation" from "interface MyAnnotation"
        assertTrue("MyAnnotation" in info.topLevelClassNames) { "got ${info.topLevelClassNames}" }
    }

    @Test
    fun testLightweightScannerToleratesUnbalancedClosingBrace(@TempDir tempDir: Path) {
        // An unmatched `}` must not shift the remaining declarations out of the top-level frame:
        // without clamping, `Foo` is missed and the file is dropped from the index entirely.
        val file = tempDir.resolve("Foo.java")
        file.writeText(
            """
            package com.example;

            class Broken {
            }
            }

            public class Foo {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals("com.example", info.packageName)
        assertEquals(setOf("Broken", "Foo"), info.topLevelClassNames)
    }

    @Test
    fun testLightweightScannerToleratesUnbalancedClosingParenthesis(@TempDir tempDir: Path) {
        val file = tempDir.resolve("Foo.java")
        file.writeText(
            """
            package com.example;

            public class Foo {
                void m() { f(1)); }
            }

            class Bar {}
        """.trimIndent()
        )

        val info = extractFileInfoLightweight(file.toFile())
        assertNotNull(info)
        assertEquals(setOf("Foo", "Bar"), info.topLevelClassNames)
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

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        // findClass should succeed (class was cached during indexing for small files)
        val classId = ClassId(FqName("com.example"), Name.identifier("Small"))
        val request = JavaClassFinder.Request(classId)
        val javaClass = finder.findClass(request)

        assertNotNull(javaClass)
        assertEquals("Small", javaClass.name.asString())
        assertEquals(1, javaClass.fields.size)
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

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        // Both classes should be findable after a single parse during indexing
        val mainId = ClassId(FqName("test"), Name.identifier("Main"))
        val helperId = ClassId(FqName("test"), Name.identifier("Helper"))

        val mainClass = finder.findClass(JavaClassFinder.Request(mainId))
        val helperClass = finder.findClass(JavaClassFinder.Request(helperId))

        assertNotNull(mainClass)
        assertNotNull(helperClass)
        assertEquals("Main", mainClass.name.asString())
        assertEquals("Helper", helperClass.name.asString())
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
        assertTrue(largeContent.toByteArray().size > 4096) { "Test file should be > 4KB" }

        pkgDir.resolve("Large.java").writeText(largeContent)

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        // Class should be indexed and findable despite using lightweight scanning
        val classId = ClassId(FqName("com.big"), Name.identifier("Large"))
        val request = JavaClassFinder.Request(classId)
        val javaClass = finder.findClass(request)

        assertNotNull(javaClass)
        assertEquals("Large", javaClass.name.asString())
        assertEquals(200, javaClass.fields.size)
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
        assertTrue(largeContent.toByteArray().size > 4096) { "Test file should be > 4KB" }

        pkgDir.resolve("BigMain.java").writeText(largeContent)

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        // First access triggers full parse, which should cache both classes
        val mainId = ClassId(FqName("test"), Name.identifier("BigMain"))
        val helperId = ClassId(FqName("test"), Name.identifier("BigHelper"))

        assertNotNull(finder.findClass(JavaClassFinder.Request(mainId)))

        // BigHelper should also be cached from the same parse (no additional file I/O)
        assertNotNull(finder.findClass(JavaClassFinder.Request(helperId)))
    }
}
