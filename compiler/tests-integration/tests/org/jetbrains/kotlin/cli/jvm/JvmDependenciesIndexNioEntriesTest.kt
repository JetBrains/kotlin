/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.VfsClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.NioRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.index.nio.PathClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.PathRootEntry
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Behavior of the NIO directory entries ([PathRootEntry]) under [JvmDependenciesIndexImpl] which the shared contract does not pin. */
class JvmDependenciesIndexNioEntriesTest : JvmDependenciesIndexContractTestBase() {
    private fun nioIndex(vararg roots: JavaRoot) = JvmDependenciesIndexImpl(roots.asList(), NioRootEntryFactory)

    @Test
    fun directoryRootsGetNioEntriesAndOtherRootsVfsEntries() {
        val out = dir("out", "p/A.class")
        val lib = jar("lib.jar", "q/B.class")
        val index = nioIndex(out.dirRoot(), lib.jarRoot())
        assertInstanceOf(PathClassFileHandle::class.java, index.findClassFiles(classId("p/A"), CLASS).single())
        assertInstanceOf(VfsClassFileHandle::class.java, index.findClassFiles(classId("q/B"), CLASS).single())
    }

    @Test
    fun namesAreMatchedCaseSensitively() {
        val out = dir("out", "p/a.class", "Q/B.class")
        val index = nioIndex(out.dirRoot())
        assertTrue(index.findClassFiles(classId("p/A"), CLASS).isEmpty())
        assertEquals("a.class", index.findClassFiles(classId("p/a"), CLASS).single().fileName())
        assertFalse(index.hasPackage(FqName("q")))
        assertTrue(index.hasPackage(FqName("Q")))
    }

    @Test
    fun directoryNamedLikeAClassWithoutClassesBelowIsNotAPackage() {
        val out = dir("out", "p/foo.class", "p/foo/readme.txt", "p/bar.class", "p/bar/baz/C.class")
        val index = nioIndex(out.dirRoot())
        assertFalse(index.hasPackage(FqName("p.foo")))
        assertEquals("foo.class", index.findClassFiles(classId("p/foo"), CLASS).single().fileName())
        assertTrue(index.hasPackage(FqName("p.bar")), "a class file anywhere below keeps the directory a package")
        assertTrue(index.hasPackage(FqName("p.bar.baz")))
        assertEquals("C.class", index.findClassFiles(classId("p/bar/baz/C"), CLASS).single().fileName())
    }

    @Test
    fun prefixedSourceRootIsFoundUnderItsPrefix() {
        val src = dir("src", "C.java", "sub/D.java")
        val index = nioIndex(src.dirRoot(JavaRoot.RootType.SOURCE, FqName("a.b")))
        assertEquals("C.java", index.findClassFiles(classId("a/b/C"), JAVA).single().fileName())
        assertEquals("D.java", index.findClassFiles(classId("a/b/sub/D"), JAVA).single().fileName())
        assertTrue(index.hasPackage(FqName("a")))
        assertFalse(index.hasPackage(FqName("sub")))
    }

    @Test
    fun handleReadsBytesAndFindsSiblingsInItsDirectory() {
        val out = dir("out", "p/A\$1.class", content = mapOf("p/A.class" to byteArrayOf(1, 2, 3)))
        val handle = nioIndex(out.dirRoot()).findClassFiles(classId("p/A"), CLASS).single()
        assertArrayEquals(byteArrayOf(1, 2, 3), handle.readBytes())
        assertEquals(JvmClasspathRootId.of(out), handle.rootId)
        assertEquals("A\$1.class", handle.sibling("A\$1.class")?.fileName())
        assertNull(handle.sibling("Missing.class"))
        assertNull(handle.sibling("a\$1.class"), "siblings are matched case-sensitively")
        assertTrue(handle.pathForDiagnostics.endsWith("A.class"))
    }

    @Test
    fun handlesAreEqualByRootAndRelativePath() {
        val out = dir("out", "p/A.class", "p/B.class")
        val first = nioIndex(out.dirRoot()).findClassFiles(classId("p/A"), CLASS).single()
        val second = nioIndex(out.dirRoot()).findClassFiles(classId("p/A"), CLASS).single()
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, nioIndex(out.dirRoot()).findClassFiles(classId("p/B"), CLASS).single())
    }

    @Test
    fun packageTraversalListsTheFilesOfAllRoots() {
        val out = dir("out", "p/A.class", "p/A.java", "p/sub/S.class")
        val lib = jar("lib.jar", "p/B.class")
        val names = mutableListOf<String>()
        nioIndex(out.dirRoot(), lib.jarRoot()).traverseFilesInPackage(FqName("p"), CLASS) { handle: BinaryClassFileHandle ->
            names += handle.fileName()
            true
        }
        assertEquals(listOf("A.class", "B.class"), names)
    }
}
