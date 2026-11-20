/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SingleJavaFileRootsIndexTest {

    private class TestJavaFile(name: String, private val content: String) : LightVirtualFile(name, content) {
        var reads = 0
            private set

        // The default implementation goes through FileSizeLimit, which requires an IntelliJ Application.
        override fun contentsToByteArray(): ByteArray {
            reads++
            return content.toByteArray()
        }
    }

    private fun javaRoot(name: String, content: String): JavaRoot =
        JavaRoot(TestJavaFile(name, content), JavaRoot.RootType.SOURCE)

    private val JavaRoot.reads: Int
        get() = (file as TestJavaFile).reads

    private val rootFirst = javaRoot(
        "A.java",
        """
        package com.example.foo.bar;
        public class A {
            public static class Nested {}
        }
        class Extra {}
        """.trimIndent(),
    )
    private val rootTypes = javaRoot(
        "Types.java",
        """
        package com.example.foo.baz;
        public interface I {}
        enum E { ONE }
        record R(int x) {}
        """.trimIndent(),
    )
    private val rootDuplicateA = javaRoot(
        "A.java",
        """
        package com.example.foo.bar;
        public class A {}
        """.trimIndent(),
    )
    private val rootPackageInfo = javaRoot(
        "package-info.java",
        """
        @Deprecated
        package com.example.pkg.sub;
        """.trimIndent(),
    )
    private val rootDefaultPackage = javaRoot(
        "RootClass.java",
        """
        public class RootClass {}
        """.trimIndent(),
    )
    private val allRoots = listOf(rootFirst, rootTypes, rootDuplicateA, rootPackageInfo, rootDefaultPackage)

    private val classA = ClassId(FqName("com.example.foo.bar"), Name.identifier("A"))
    private val classExtra = ClassId(FqName("com.example.foo.bar"), Name.identifier("Extra"))
    private val interfaceI = ClassId(FqName("com.example.foo.baz"), Name.identifier("I"))
    private val enumE = ClassId(FqName("com.example.foo.baz"), Name.identifier("E"))
    private val recordR = ClassId(FqName("com.example.foo.baz"), Name.identifier("R"))
    private val packageInfo = ClassId(FqName("com.example.pkg.sub"), SingleJavaFileRootsIndex.PACKAGE_INFO_CLASS_NAME)
    private val rootClass = ClassId(FqName.ROOT, Name.identifier("RootClass"))

    @Test
    fun `empty roots list`() {
        val index = SingleJavaFileRootsIndex(emptyList())

        assertNull(index.findJavaSourceClass(classA))
        assertFalse(index.hasPackage(FqName("com.example")))
        assertFalse(index.hasPackage(FqName.ROOT))
        assertEquals(emptyList<ClassId>(), index.findJavaSourceClasses(FqName("com.example")))
        assertEquals(emptyList<ClassId>(), index.findJavaSourceClasses(FqName.ROOT))
    }

    @Test
    fun `findJavaSourceClass finds top-level classes and package-info`() {
        val index = SingleJavaFileRootsIndex(allRoots)

        assertSame(rootFirst.file, index.findJavaSourceClass(classA))
        assertSame(rootFirst.file, index.findJavaSourceClass(classExtra))
        assertSame(rootTypes.file, index.findJavaSourceClass(interfaceI))
        assertSame(rootTypes.file, index.findJavaSourceClass(enumE))
        assertSame(rootTypes.file, index.findJavaSourceClass(recordR))
        assertSame(rootPackageInfo.file, index.findJavaSourceClass(packageInfo))
        assertSame(rootDefaultPackage.file, index.findJavaSourceClass(rootClass))
    }

    @Test
    fun `findJavaSourceClass prefers the first root declaring a class`() {
        val index = SingleJavaFileRootsIndex(listOf(rootDuplicateA, rootFirst))

        assertSame(rootDuplicateA.file, index.findJavaSourceClass(classA))
        assertSame(rootFirst.file, index.findJavaSourceClass(classExtra))
        assertSame(rootDuplicateA.file, index.findJavaSourceClass(classA))
    }

    @Test
    fun `findJavaSourceClass returns null for nested and unknown classes`() {
        val index = SingleJavaFileRootsIndex(allRoots)

        assertNull(index.findJavaSourceClass(classA.createNestedClassId(Name.identifier("Nested"))))
        assertNull(index.findJavaSourceClass(ClassId(FqName("com.example.foo.bar"), Name.identifier("Missing"))))
        assertNull(index.findJavaSourceClass(ClassId(FqName("com.example"), Name.identifier("A"))))
    }

    @Test
    fun `hasPackage matches declared packages and their parents`() {
        val index = SingleJavaFileRootsIndex(allRoots)

        assertTrue(index.hasPackage(FqName("com")))
        assertTrue(index.hasPackage(FqName("com.example")))
        assertTrue(index.hasPackage(FqName("com.example.foo")))
        assertTrue(index.hasPackage(FqName("com.example.foo.bar")))
        assertTrue(index.hasPackage(FqName("com.example.foo.baz")))
        assertTrue(index.hasPackage(FqName("com.example.pkg")))
        assertTrue(index.hasPackage(FqName("com.example.pkg.sub")))

        assertFalse(index.hasPackage(FqName("com.example.fo")))
        assertFalse(index.hasPackage(FqName("com.example.foo.qux")))
        assertFalse(index.hasPackage(FqName("com.example.foo.bar.A")))
        assertFalse(index.hasPackage(FqName("org.other")))
        assertFalse(index.hasPackage(FqName.ROOT))
    }

    @Test
    fun `findJavaSourceClasses returns classes of exactly that package in root order`() {
        val index = SingleJavaFileRootsIndex(allRoots)

        assertEquals(listOf(classA, classExtra, classA), index.findJavaSourceClasses(FqName("com.example.foo.bar")))
        assertEquals(listOf(interfaceI, enumE, recordR), index.findJavaSourceClasses(FqName("com.example.foo.baz")))
        assertEquals(listOf(packageInfo), index.findJavaSourceClasses(FqName("com.example.pkg.sub")))
        assertEquals(listOf(rootClass), index.findJavaSourceClasses(FqName.ROOT))
        assertEquals(emptyList<ClassId>(), index.findJavaSourceClasses(FqName("com.example.foo")))
        assertEquals(emptyList<ClassId>(), index.findJavaSourceClasses(FqName("org.other")))
    }

    @Test
    fun `roots are lexed lazily and at most once`() {
        val index = SingleJavaFileRootsIndex(allRoots)
        fun reads() = allRoots.map { it.reads }

        assertEquals(listOf(0, 0, 0, 0, 0), reads())

        assertSame(rootFirst.file, index.findJavaSourceClass(classA))
        assertEquals(listOf(1, 0, 0, 0, 0), reads())

        assertSame(rootTypes.file, index.findJavaSourceClass(recordR))
        assertEquals(listOf(1, 1, 0, 0, 0), reads())

        assertTrue(index.hasPackage(FqName("com.example.foo")))
        assertEquals(listOf(1, 1, 0, 0, 0), reads())

        assertTrue(index.hasPackage(FqName("com.example.pkg")))
        assertEquals(listOf(1, 1, 1, 1, 0), reads())

        assertEquals(listOf(rootClass), index.findJavaSourceClasses(FqName.ROOT))
        assertEquals(listOf(1, 1, 1, 1, 1), reads())

        assertSame(rootFirst.file, index.findJavaSourceClass(classA))
        assertNull(index.findJavaSourceClass(ClassId(FqName("com.example.foo.bar"), Name.identifier("Missing"))))
        assertFalse(index.hasPackage(FqName("org.other")))
        assertEquals(listOf(classA, classExtra, classA), index.findJavaSourceClasses(FqName("com.example.foo.bar")))
        assertEquals(listOf(1, 1, 1, 1, 1), reads())
    }
}
