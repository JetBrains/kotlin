/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.index.nio.CtSymRootEntry
import org.jetbrains.kotlin.cli.jvm.index.nio.JarClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.JarFilePool
import org.jetbrains.kotlin.cli.jvm.index.nio.PathClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.PathRootEntry
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files

/** JRT entries ([PathRootEntry] over `jrt:/`) and `ct.sym` entries ([CtSymRootEntry] over [JarFilePool] entries). */
class JvmDependenciesIndexJrtAndCtSymEntriesTest : JvmDependenciesIndexContractTestBase() {
    private val pool = checkNotNull(JarFilePool.createIfUnmappingPossible())

    @AfterEach
    fun closePool() = pool.close()

    /** A root whose entry is [entry]; the id of the root only serves as its identity. */
    private fun rootWithEntry(id: String, type: JavaRoot.RootType, entry: JvmRootEntry): Pair<JavaRoot, JvmRootEntry> =
        JavaRoot(JvmClasspathRootId(id), type) to entry

    private fun index(vararg roots: Pair<JavaRoot, JvmRootEntry>): JvmDependenciesIndexImpl {
        val entries = roots.toMap()
        return JvmDependenciesIndexImpl(roots.map { it.first }, JvmRootEntryFactory { entries.getValue(it) })
    }

    @Test
    fun jrtModuleEntriesFindAndReadJdkClasses() {
        val jrt = try {
            FileSystems.getFileSystem(URI.create("jrt:/"))
        } catch (_: Exception) {
            null
        }
        assumeTrue(jrt != null, "needs a JDK with a jrt:/ file system")
        val javaBase = jrt!!.getPath("/modules/java.base")
        assumeTrue(Files.isDirectory(javaBase))

        val javaBaseRoot = rootWithEntry(
            "jdk!/modules/java.base", JavaRoot.RootType.BINARY, PathRootEntry.root(javaBase, "jdk!/modules/java.base")
        )
        val index = index(javaBaseRoot)
        assertTrue(index.hasPackage(FqName("java.lang")))
        val objectClass = index.findClassFiles(classId("java/lang/Object"), CLASS).single()
        assertInstanceOf(PathClassFileHandle::class.java, objectClass)
        assertEquals("jdk!/modules/java.base/java/lang/Object.class", objectClass.pathForDiagnostics)
        val magic = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
        assertArrayEquals(magic, objectClass.readBytes().copyOf(4))
        assertTrue(index.findClassFiles(classId("java/lang/object"), CLASS).isEmpty())
        assertTrue(objectClass.sibling("Object\$Missing.class") == null)
        assertTrue(index.findClassFiles(classId("java/util/Map.Entry"), CLASS).single().fileName() == "Map\$Entry.class")
    }

    private fun ctSym() = pool.rootEntry(
        jar(
            "ct.sym",
            "9/java.base/java/Top.sig",
            "9/java.base/java/lang/Object.sig",
            "9/java.base/java/lang/Object\$1.sig",
            "9/java.base/java/lang/internal/Hidden.sig",
            "9/java.base/sun/misc/Unsafe.sig",
            "A/java.base/java/lang/Record.sig",
            "9/java.desktop/java/awt/Frame.sig",
        )
    )

    private val javaBasePackages = mapOf("java" to false, "java.lang" to true)

    private fun ctSymIndex(id: String, parts: List<JvmRootEntry>, packages: Map<String, Boolean>, skipPackageCheck: Boolean) =
        index(rootWithEntry(id, JavaRoot.RootType.BINARY_SIG, CtSymRootEntry.root(parts, packages, skipPackageCheck)))

    @Test
    fun ctSymModuleRootMergesReleaseFoldersAndHidesNonExportedPackages() {
        val ctSym = ctSym()
        val parts = listOf(ctSym.findByRelativePath("9/java.base")!!, ctSym.findByRelativePath("A/java.base")!!)
        val index = ctSymIndex("ct.sym!/10/java.base", parts, javaBasePackages, false)

        assertTrue(index.hasPackage(FqName("java.lang")))
        val objectSig = index.findClassFiles(classId("java/lang/Object"), SIG).single()
        assertInstanceOf(JarClassFileHandle::class.java, objectSig)
        assertEquals(JvmClasspathRootId("ct.sym!/10/java.base"), objectSig.rootId)
        assertEquals("Object\$1.sig", objectSig.sibling("Object\$1.sig")?.fileName())
        val record = index.findClassFiles(classId("java/lang/Record"), SIG).single()
        assertEquals("Record.sig", record.fileName(), "merged from release A")

        assertTrue(index.findClassFiles(classId("java/Top"), SIG).isEmpty(), "java is not exported")
        assertFalse(index.hasPackage(FqName("java.lang.internal")))
        assertFalse(index.hasPackage(FqName("sun.misc")))
        assertTrue(index.findClassFiles(classId("java/awt/Frame"), SIG).isEmpty(), "another module")

        val names = mutableListOf<String>()
        index.traverseFilesInPackage(FqName("java.lang"), SIG) { names += it.fileName(); true }
        assertEquals(listOf("Object.sig", "Object\$1.sig", "Record.sig"), names)
    }

    @Test
    fun ctSymRootWithoutPackageCheckShowsAllPackages() {
        val ctSym = ctSym()
        val parts = listOf(ctSym.findByRelativePath("9/java.base")!!)
        val index = ctSymIndex("ct.sym!/9/*", parts, emptyMap(), true)
        assertEquals("Top.sig", index.findClassFiles(classId("java/Top"), SIG).single().fileName())
        assertEquals("Unsafe.sig", index.findClassFiles(classId("sun/misc/Unsafe"), SIG).single().fileName())
        assertTrue(index.hasPackage(FqName("java.lang.internal")))
    }

    @Test
    fun ctSymRootsSharingOneArchiveHaveDistinctHandles() {
        val ctSym = ctSym()
        val parts = listOf(ctSym.findByRelativePath("9/java.base")!!)
        val first = ctSymIndex("ct.sym!/9/java.base", parts, javaBasePackages, false)
        val second = ctSymIndex("ct.sym!/10/java.base", parts, javaBasePackages, false)
        assertNotEquals(
            first.findClassFiles(classId("java/lang/Object"), SIG).single(),
            second.findClassFiles(classId("java/lang/Object"), SIG).single(),
        )
    }
}
