/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.jvm.compiler.CliBinaryClassFileIndex
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JavaRootRegistry
import org.jetbrains.kotlin.cli.jvm.index.JvmClasspathRootFilter
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.JvmIndexKotlinClassFinder
import org.jetbrains.kotlin.cli.jvm.index.nio.JarFilePool
import org.jetbrains.kotlin.cli.jvm.index.nio.NioRootEntryFactory
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.asJvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClassCache
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.testFramework.disposeRootDisposable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

/**
 * Temp directories, jar/directory roots in the core VFS and their cleanup, shared by the contract tests below.
 */
abstract class JvmDependenciesIndexContractTestBase {
    @TempDir
    lateinit var tempDir: Path

    private val testRootDisposable: Disposable = Disposer.newDisposable()

    /** Registers the roots the tests create; the indexes and root entry factories below read their files through it. */
    protected val rootRegistry = JavaRootRegistry()

    @BeforeEach
    fun setUpApplication() {
        @OptIn(CoreEnvironmentDeprecation::class)
        KotlinCoreApplicationEnvironment.getOrCreateApplicationEnvironment(testRootDisposable)
    }

    @AfterEach
    fun tearDown() {
        disposeRootDisposable(testRootDisposable)
        // Opened archives are cached process-wide, which would keep `TempDir` from deleting them on Windows.
        ZipHandler.clearFileAccessorCache()
    }

    /** Creates a directory under [tempDir] holding [files] (relative path to content; empty content by default). */
    protected fun dir(name: String, vararg files: String, content: Map<String, ByteArray> = emptyMap()): Path {
        val dir = tempDir.resolve(name).createDirectories()
        for (file in files) dir.resolve(file).createParentsAndWrite(ByteArray(0))
        for (entry in content) dir.resolve(entry.key).createParentsAndWrite(entry.value)
        return dir
    }

    /** Creates a jar under [tempDir] with the given entries (empty content by default). */
    protected fun jar(name: String, vararg entries: String, content: Map<String, ByteArray> = emptyMap()): Path {
        val jar = tempDir.resolve(name)
        jar.parent.createDirectories()
        ZipOutputStream(jar.outputStream()).use { out ->
            for (entry in entries) {
                out.putNextEntry(ZipEntry(entry))
                out.closeEntry()
            }
            for (entry in content) {
                out.putNextEntry(ZipEntry(entry.key))
                out.write(entry.value)
                out.closeEntry()
            }
        }
        return jar
    }

    protected fun Path.createParentsAndWrite(bytes: ByteArray): Path = apply {
        parent.createDirectories()
        writeBytes(bytes)
    }

    protected fun Path.asLocalVirtualFile(): VirtualFile =
        checkNotNull(CoreLocalFileSystem().findFileByPath(pathForVfs())) { "not in the VFS: " + pathForVfs() }

    protected fun Path.asJarRootVirtualFile(): VirtualFile {
        val path = pathForVfs() + JAR_SEPARATOR
        return checkNotNull(CoreJarFileSystem().findFileByPath(path)) { "not in the VFS: $path" }
    }

    protected fun Path.pathForVfs(): String = toAbsolutePath().normalize().toString().replace('\\', '/')

    protected fun Path.dirRoot(type: JavaRoot.RootType = JavaRoot.RootType.BINARY, prefix: FqName? = null): JavaRoot =
        rootRegistry.javaRoot(asLocalVirtualFile(), type, prefix)

    protected fun Path.jarRoot(type: JavaRoot.RootType = JavaRoot.RootType.BINARY): JavaRoot =
        rootRegistry.javaRoot(asJarRootVirtualFile(), type)

    protected companion object {
        val CLASS = JavaFileExtensions(JavaFileExtension.CLASS)
        val SIG = JavaFileExtensions(JavaFileExtension.SIG)
        val JAVA = JavaFileExtensions(JavaFileExtension.JAVA)
        val CLASS_AND_SIG = JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.SIG)
        val SIG_AND_CLASS = JavaFileExtensions(JavaFileExtension.SIG, JavaFileExtension.CLASS)
        val JAVA_AND_CLASS = JavaFileExtensions(JavaFileExtension.JAVA, JavaFileExtension.CLASS)
        val CLASS_AND_JAVA = JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.JAVA)
        val CLASS_AND_METADATA = JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.KOTLIN_METADATA)
        val METADATA_AND_CLASS = JavaFileExtensions(JavaFileExtension.KOTLIN_METADATA, JavaFileExtension.CLASS)

        fun classId(s: String): ClassId = ClassId.fromString(s)
        fun fq(s: String): FqName = if (s.isEmpty()) FqName.ROOT else FqName(s)

        fun JvmDependenciesIndex.files(pkg: String, extensions: JavaFileExtensions): List<BinaryClassFileHandle> {
            val result = mutableListOf<BinaryClassFileHandle>()
            traverseFilesInPackage(fq(pkg), extensions) { result.add(it); true }
            return result
        }

        fun JvmDependenciesIndex.classNames(pkg: String, extensions: JavaFileExtensions): List<String> =
            files(pkg, extensions).map { it.nameWithoutExtension }

        fun BinaryClassFileHandle.fileName(): String = "$nameWithoutExtension.$extension"
    }
}

/**
 * The parity contract of [JvmDependenciesIndex] as the CLI uses it.
 */
abstract class AbstractJvmDependenciesIndexContractTest : JvmDependenciesIndexContractTestBase() {
    protected abstract fun createIndex(roots: List<JavaRoot>): JvmDependenciesIndex

    private fun finder(index: JvmDependenciesIndex, classpath: JvmClasspath): JvmIndexKotlinClassFinder =
        JvmIndexKotlinClassFinder(index, classpath, enableSearchInCtSym = false, perfManager = null)

    private fun finder(index: JvmDependenciesIndex, classpath: List<JvmClasspathRootId>): JvmIndexKotlinClassFinder =
        finder(index, JvmClasspath.Roots(classpath))

    private fun finder(index: JvmDependenciesIndex): JvmIndexKotlinClassFinder = finder(index, emptyList())

    private fun finder(index: JvmDependenciesIndex, root: JvmClasspathRootId): JvmIndexKotlinClassFinder = finder(index, listOf(root))

    private fun finder(index: JvmDependenciesIndex, first: JvmClasspathRootId, second: JvmClasspathRootId): JvmIndexKotlinClassFinder =
        finder(index, listOf(first, second))

    // ---- 1. Root order / first match ----

    @Test
    fun firstRootWinsBetweenDirectoryAndJar() {
        val out = dir("out", "p/A.class")
        val lib = jar("lib.jar", "p/A.class")
        val dirFirst = createIndex(listOf(out.dirRoot(), lib.jarRoot()))
        assertEquals(JvmClasspathRootId.of(out), dirFirst.findClassFiles(classId("p/A"), CLASS).single().rootId)

        val jarFirst = createIndex(listOf(lib.jarRoot(), out.dirRoot()))
        assertEquals(JvmClasspathRootId.of(lib), jarFirst.findClassFiles(classId("p/A"), CLASS).single().rootId)
    }

    @Test
    fun laterRootIsSearchedWhenEarlierRootLacksTheClass() {
        val out = dir("out", "p/Other.class")
        val lib = jar("lib.jar", "p/A.class")
        val index = createIndex(listOf(out.dirRoot(), lib.jarRoot()))
        assertEquals(JvmClasspathRootId.of(lib), index.findClassFiles(classId("p/A"), CLASS).single().rootId)
        assertTrue(index.findClassFiles(classId("p/Missing"), CLASS).isEmpty())
    }

    @Test
    fun rootOrderTakesPrecedenceOverExtensionOrderWithSourceRootFirst() {
        val src = dir("src", "p/A.java")
        val bin = dir("bin", "p/A.class")
        val index = createIndex(listOf(src.dirRoot(JavaRoot.RootType.SOURCE), bin.dirRoot()))
        assertEquals("A.java", index.findClassFiles(classId("p/A"), JAVA_AND_CLASS).single().fileName())
        assertEquals(
            "A.java", index.findClassFiles(classId("p/A"), CLASS_AND_JAVA).single().fileName(),
            "the source root comes first, so extension order does not matter"
        )
        assertEquals(JvmClasspathRootId.of(src), index.findClassFiles(classId("p/A"), JAVA_AND_CLASS).single().rootId)
        assertEquals("A.class", index.findClassFiles(classId("p/A"), CLASS).single().fileName())
    }

    @Test
    fun extensionOrderDecidesWithinOneRoot() {
        val bin = dir("bin", "p/A.class", "p/A.kotlin_metadata")
        val index = createIndex(listOf(bin.dirRoot()))
        assertEquals("A.class", index.findClassFiles(classId("p/A"), CLASS_AND_METADATA).single().fileName())
        assertEquals("A.kotlin_metadata", index.findClassFiles(classId("p/A"), METADATA_AND_CLASS).single().fileName())
    }

    @Test
    fun extensionIsOnlyTriedInRootsOfItsRootType() {
        // A `.sig` file in a BINARY root and a `.class` file in a BINARY_SIG root are both invisible.
        val bin = dir("bin", "p/A.sig")
        val sig = dir("sig", "p/A.class", "p/B.sig")
        val index = createIndex(listOf(bin.dirRoot(), sig.dirRoot(JavaRoot.RootType.BINARY_SIG)))
        assertTrue(index.findClassFiles(classId("p/A"), CLASS_AND_SIG).isEmpty())
        assertTrue(index.findClassFiles(classId("p/A"), SIG_AND_CLASS).isEmpty())
        assertEquals(JvmClasspathRootId.of(sig), index.findClassFiles(classId("p/B"), CLASS_AND_SIG).single().rootId)
        assertTrue(index.findClassFiles(classId("p/B"), CLASS).isEmpty(), "a BINARY_SIG root is skipped without SIG")
    }

    @Test
    fun binaryRootBeforeSigRootWinsRegardlessOfExtensionOrder() {
        val bin = dir("bin", "p/A.class")
        val sig = dir("sig", "p/A.sig")
        val index = createIndex(listOf(bin.dirRoot(), sig.dirRoot(JavaRoot.RootType.BINARY_SIG)))
        assertEquals("A.class", index.findClassFiles(classId("p/A"), SIG_AND_CLASS).single().fileName())
        assertEquals("A.sig", index.findClassFiles(classId("p/A"), SIG).single().fileName())
    }

    @Test
    fun nestedClassIdIsLookedUpAsDollarFileName() {
        val bin = dir("bin", "p/A\$B.class")
        val index = createIndex(listOf(bin.dirRoot()))
        assertEquals("A\$B.class", index.findClassFiles(classId("p/A.B"), CLASS).single().fileName())
    }

    // ---- 2. Class-vs-package rule ----

    @Test
    fun emptyDirectoryNextToClassFileIsNotAPackage() {
        val bin = dir("bin", "p/foo.class")
        bin.resolve("p/foo").createDirectories()
        val index = createIndex(listOf(bin.dirRoot()))
        assertFalse(index.hasPackage(fq("p.foo")))
        assertEquals("foo.class", index.findClassFiles(classId("p/foo"), CLASS).single().fileName())
    }

    @Test
    fun directoryWithOnlyNonClassFilesNextToClassFileIsNotAPackage() {
        val bin = dir("bin", "p/foo.class", "p/foo/readme.txt", "p/foo/sub/X.java", "p/foo/Y.kotlin_metadata")
        val index = createIndex(listOf(bin.dirRoot()))
        assertFalse(index.hasPackage(fq("p.foo")))
        assertFalse(index.hasPackage(fq("p.foo.sub")))
        assertTrue(index.classNames("p.foo", JavaFileExtensions(JavaFileExtension.KOTLIN_METADATA)).isEmpty())
        assertEquals("foo.class", index.findClassFiles(classId("p/foo"), CLASS).single().fileName())
    }

    @Test
    fun directoryWithClassFilesNextToClassFileIsAPackage() {
        val bin = dir("bin", "p/foo.class", "p/foo/Bar.class")
        val index = createIndex(listOf(bin.dirRoot()))
        assertTrue(index.hasPackage(fq("p.foo")))
        assertEquals("Bar.class", index.findClassFiles(classId("p.foo/Bar"), CLASS).single().fileName())
        assertEquals("foo.class", index.findClassFiles(classId("p/foo"), CLASS).single().fileName(), "the class is still found")
    }

    @Test
    fun directoryWithClassFilesOnlyInSubdirectoryNextToClassFileIsAPackage() {
        val bin = dir("bin", "p/foo.class", "p/foo/sub/Bar.class")
        val index = createIndex(listOf(bin.dirRoot()))
        assertTrue(index.hasPackage(fq("p.foo")), "class files are searched recursively")
        assertTrue(index.hasPackage(fq("p.foo.sub")))
    }

    @Test
    fun directoryWithoutSameNamedClassFileIsAlwaysAPackage() {
        val bin = dir("bin", "p/foo/readme.txt")
        val index = createIndex(listOf(bin.dirRoot()))
        assertTrue(index.hasPackage(fq("p.foo")))
    }

    @Test
    fun classVsPackageRuleUsesTheExtensionOfTheRootType() {
        // In a SOURCE root, `foo.java` hides a directory `foo` that has no `.java` files, even if it has `.class` files.
        val src = dir("src", "p/foo.java", "p/foo/Bar.class")
        val index = createIndex(listOf(src.dirRoot(JavaRoot.RootType.SOURCE)))
        assertFalse(index.hasPackage(fq("p.foo")))

        val src2 = dir("src2", "p/foo.java", "p/foo/Bar.java")
        assertTrue(createIndex(listOf(src2.dirRoot(JavaRoot.RootType.SOURCE))).hasPackage(fq("p.foo")))
    }

    @Test
    fun classVsPackageRuleIsPerRoot() {
        val first = dir("first", "p/foo.class")
        first.resolve("p/foo").createDirectories()
        val second = dir("second", "p/foo/Bar.class")
        val index = createIndex(listOf(first.dirRoot(), second.dirRoot()))
        assertTrue(index.hasPackage(fq("p.foo")))
        assertEquals(JvmClasspathRootId.of(second), index.findClassFiles(classId("p.foo/Bar"), CLASS).single().rootId)
    }

    // ---- 3. traverseFilesInPackage ----

    @Test
    fun traverseHandsOutHandlesOfTheListedFiles() {
        val bin = dir("bin", content = mapOf("p/A.class" to byteArrayOf(1), "p/M.kotlin_metadata" to byteArrayOf(2)))
        val lib = jar("lib.jar", content = mapOf("p/A.class" to byteArrayOf(3)))
        val index = createIndex(listOf(bin.dirRoot(), lib.jarRoot()))
        val files = index.files("p", CLASS_AND_METADATA).sortedBy { it.fileName() }
        assertEquals(listOf("A.class", "A.class", "M.kotlin_metadata"), files.map { it.fileName() })
        assertEquals(
            setOf(JvmClasspathRootId.of(bin) to 1.toByte(), JvmClasspathRootId.of(lib) to 3.toByte()),
            files.filter { it.extension == "class" }.map { it.rootId to it.readBytes().single() }.toSet()
        )
        assertArrayEquals(byteArrayOf(2), files.last().readBytes())
        assertEquals(JvmClasspathRootId.of(bin), files.last().rootId)
    }

    @Test
    fun traverseReportsNamesWithoutExtensionOnlyForAcceptedExtensions() {
        val bin = dir("bin", "p/A.class", "p/B.class", "p/C.txt", "p/D.java", "p/E.sig", "p/noext", "p/sub/F.class")
        val index = createIndex(listOf(bin.dirRoot()))
        assertEquals(listOf("A", "B"), index.classNames("p", CLASS).sorted())
        assertEquals(
            listOf("A", "B", "E"), index.classNames("p", CLASS_AND_SIG).sorted(),
            "unlike findClassFiles, traversal reports any accepted extension in a root of any accepted root type"
        )
        assertTrue(index.classNames("p", JAVA).isEmpty(), "JAVA is only tried in SOURCE roots")
    }

    @Test
    fun traverseOnlyConsidersRootsOfTheExtensionsRootTypes() {
        val bin = dir("bin", "p/A.class")
        val src = dir("src", "p/B.java", "p/C.class")
        val sig = dir("sig", "p/D.sig", "p/E.class")
        val index = createIndex(listOf(bin.dirRoot(), src.dirRoot(JavaRoot.RootType.SOURCE), sig.dirRoot(JavaRoot.RootType.BINARY_SIG)))
        assertEquals(listOf("A"), index.classNames("p", CLASS))
        assertEquals(listOf("B"), index.classNames("p", JAVA))
        assertEquals(listOf("D"), index.classNames("p", SIG))
        assertEquals(
            listOf("A", "D", "E"), index.classNames("p", CLASS_AND_SIG).sorted(),
            "within an accepted root, any accepted extension is reported, whatever its root type"
        )
        assertEquals(listOf("A", "B", "C"), index.classNames("p", JAVA_AND_CLASS).sorted())
    }

    @Test
    fun traverseReportsDuplicateNamesOncePerRootInRootOrder() {
        val first = dir("first", "p/A.class")
        val lib = jar("lib.jar", "p/A.class", "p/B.class")
        val third = dir("third", "p/A.class")
        val index = createIndex(listOf(first.dirRoot(), lib.jarRoot(), third.dirRoot()))
        val names = index.classNames("p", CLASS)
        assertEquals(listOf("A", "A", "A", "B"), names.sorted())
        assertEquals("A", names.first(), "the first root is traversed first")
        assertEquals("A", names.last(), "the last root is traversed last")
    }

    @Test
    fun traverseStopsWhenCallbackReturnsFalse() {
        val first = dir("first", "p/A.class", "p/B.class")
        val second = dir("second", "p/C.class")
        val index = createIndex(listOf(first.dirRoot(), second.dirRoot()))
        val seen = mutableListOf<BinaryClassFileHandle>()
        index.traverseFilesInPackage(fq("p"), CLASS) { seen.add(it); false }
        assertEquals(1, seen.size)

        val seenUpToSecond = mutableListOf<BinaryClassFileHandle>()
        index.traverseFilesInPackage(fq("p"), CLASS) { seenUpToSecond.add(it); seenUpToSecond.size < 2 }
        assertEquals(2, seenUpToSecond.size)
        assertEquals(
            listOf(JvmClasspathRootId.of(first)), seenUpToSecond.map { it.rootId }.distinct(),
            "the traversal does not go on to the next root"
        )
    }

    @Test
    fun traverseOfMissingPackageReportsNothing() {
        val index = createIndex(listOf(dir("bin", "p/A.class").dirRoot()))
        assertTrue(index.classNames("q", CLASS).isEmpty())
        assertEquals(listOf("A"), createIndex(listOf(dir("bin2", "A.class").dirRoot())).classNames("", CLASS), "default package")
    }

    // ---- 4. hasPackage, default package, prefixFqName ----

    @Test
    fun hasPackageRespectsAcceptedRootTypes() {
        val src = dir("src", "s/A.java")
        val bin = dir("bin", "b/A.class")
        val index = createIndex(listOf(src.dirRoot(JavaRoot.RootType.SOURCE), bin.dirRoot()))
        assertTrue(index.hasPackage(fq("s")))
        assertTrue(index.hasPackage(fq("s"), JavaRoot.OnlySource))
        assertFalse(index.hasPackage(fq("s"), JavaRoot.OnlyBinary))
        assertTrue(index.hasPackage(fq("b"), JavaRoot.OnlyBinary))
        assertFalse(index.hasPackage(fq("b"), JavaRoot.OnlySource))
        assertFalse(index.hasPackage(fq("b"), emptySet()))
        assertFalse(index.hasPackage(fq("missing")))
    }

    @Test
    fun packageIsFoundInJarRootsAndForIntermediateDirectories() {
        val lib = jar("lib.jar", "a/b/c/A.class")
        val index = createIndex(listOf(lib.jarRoot()))
        assertTrue(index.hasPackage(fq("a")))
        assertTrue(index.hasPackage(fq("a.b")))
        assertTrue(index.hasPackage(fq("a.b.c")))
        assertFalse(index.hasPackage(fq("a.b.c.d")))
    }

    @Test
    fun defaultPackageExistsInEveryRootOfAnAcceptedType() {
        val src = dir("src")
        val index = createIndex(listOf(src.dirRoot(JavaRoot.RootType.SOURCE)))
        assertTrue(index.hasPackage(FqName.ROOT))
        assertTrue(index.hasPackage(FqName.ROOT, JavaRoot.OnlySource))
        assertFalse(index.hasPackage(FqName.ROOT, JavaRoot.OnlyBinary))
        assertFalse(createIndex(emptyList()).hasPackage(FqName.ROOT))
    }

    @Test
    fun prefixedSourceRootAnswersItsPrefixPackagesAndClassesUnderIt() {
        val src = dir("src", "C.java", "c/D.java")
        val index = createIndex(listOf(src.dirRoot(JavaRoot.RootType.SOURCE, prefix = fq("a.b"))))
        assertTrue(index.hasPackage(fq("a")))
        assertTrue(index.hasPackage(fq("a.b")))
        assertTrue(index.hasPackage(fq("a.b.c")))
        assertFalse(index.hasPackage(fq("x")))
        assertFalse(index.hasPackage(fq("a.x")))
        assertFalse(index.hasPackage(fq("a.b.x")))
        assertFalse(index.hasPackage(fq("c")), "the real directory is not visible without the prefix")

        assertEquals("C.java", index.findClassFiles(classId("a.b/C"), JAVA).single().fileName())
        assertEquals("D.java", index.findClassFiles(classId("a.b.c/D"), JAVA).single().fileName())
        assertTrue(index.findClassFiles(classId("x.b/C"), JAVA).isEmpty())
        assertTrue(index.findClassFiles(classId("c/D"), JAVA).isEmpty())
        assertEquals(listOf("C"), index.classNames("a.b", JAVA))
    }

    @Test
    fun currentBehavior_prefixedRootDirectoryAlsoAnswersShorterPrefixPackages() {
        val src = dir("src", "C.java")
        val index = createIndex(listOf(src.dirRoot(JavaRoot.RootType.SOURCE, prefix = fq("a.b"))))
        assertEquals("C.java", index.findClassFiles(classId("a/C"), JAVA).single().fileName())
        assertEquals("C.java", index.findClassFiles(classId("C"), JAVA).single().fileName())
        assertEquals(listOf("C"), index.classNames("a", JAVA))
        assertEquals(listOf("C"), index.classNames("", JAVA))
    }

    // ---- 5. findBuiltInsFile ----

    @Test
    fun findBuiltInsFileReturnsTheFileOfTheFirstRoot() {
        val first = jar("first.jar", content = mapOf("kotlin/kotlin.kotlin_builtins" to byteArrayOf(1, 2, 3)))
        val second = dir("second", content = mapOf("kotlin/kotlin.kotlin_builtins" to byteArrayOf(4, 5)))
        val index = createIndex(listOf(first.jarRoot(), second.dirRoot()))
        val file = checkNotNull(index.findBuiltInsFile(fq("kotlin")))
        assertEquals(JvmClasspathRootId.of(first), file.rootId)
        assertArrayEquals(byteArrayOf(1, 2, 3), file.readBytes())

        val reversed = createIndex(listOf(second.dirRoot(), first.jarRoot()))
        assertArrayEquals(
            byteArrayOf(4, 5),
            reversed.findBuiltInsFile(fq("kotlin"))!!.readBytes()
        )
    }

    @Test
    fun findBuiltInsFileReturnsNullForMissingFileOrPackageOrSourceRoot() {
        val src = dir("src", content = mapOf("kotlin/kotlin.kotlin_builtins" to byteArrayOf(9)))
        val bin = dir("bin", "kotlin/Other.class", content = mapOf("kotlin/other.kotlin_builtins" to byteArrayOf(1)))
        val index = createIndex(listOf(src.dirRoot(JavaRoot.RootType.SOURCE), bin.dirRoot()))
        assertNull(index.findBuiltInsFile(fq("kotlin")), "only binary roots are searched, for the file named after the package")
        assertNull(index.findBuiltInsFile(fq("missing")))
        assertNotNull(createIndex(listOf(src.dirRoot())).findBuiltInsFile(fq("kotlin")))
    }

    @Test
    fun findBuiltInsDataThroughFinderIsNotRestrictedByClasspath() {
        val first = jar("first.jar", content = mapOf("kotlin/kotlin.kotlin_builtins" to byteArrayOf(7, 7)))
        val second = jar("second.jar", content = mapOf("kotlin/kotlin.kotlin_builtins" to byteArrayOf(8)))
        val index = createIndex(listOf(first.jarRoot(), second.jarRoot()))
        val restricted = finder(index, JvmClasspathRootId.of(second))
        assertArrayEquals(byteArrayOf(7, 7), restricted.findBuiltInsData(fq("kotlin"))!!.readBytes())
        assertArrayEquals(byteArrayOf(7, 7), finder(index).findBuiltInsData(fq("kotlin"))!!.readBytes())
        assertNull(restricted.findBuiltInsData(fq("other")))
    }

    // ---- 6. .kotlin_metadata via JvmIndexKotlinClassFinder ----

    @Test
    fun metadataPackageQueriesThroughFinderAreIndexWideNotRestrictedByClasspath() {
        val outside = dir("outside", "p/M.kotlin_metadata", "p/N.kotlin_metadata", "q/Q.kotlin_metadata", "p/C.class")
        val inside = jar("inside.jar", "p/M.kotlin_metadata")
        val index = createIndex(listOf(outside.dirRoot(), inside.jarRoot()))
        val finder = finder(index, JvmClasspathRootId.of(inside))
        assertEquals(setOf("M", "N"), finder.findMetadataTopLevelClassesInPackage(fq("p")))
        assertTrue(finder.hasMetadataPackage(fq("q")), "q only exists outside the classpath")
        assertFalse(finder.hasMetadataPackage(fq("missing")))
        assertTrue(finder(index).findMetadataTopLevelClassesInPackage(fq("p")).isNotEmpty())
    }

    /**
     * Unlike the package-level metadata queries, [JvmIndexKotlinClassFinder.findMetadata] goes through the classpath filter, which is
     * applied during the traversal, as for class files (KT-91439).
     */
    @Test
    fun findMetadataIsFilteredByClasspathDuringTraversal() {
        val outside = dir("outside", content = mapOf("p/M.kotlin_metadata" to byteArrayOf(1)))
        val inside = jar("inside.jar", content = mapOf("p/M.kotlin_metadata" to byteArrayOf(2), "p/N.kotlin_metadata" to byteArrayOf(3)))
        val index = createIndex(listOf(outside.dirRoot(), inside.jarRoot()))
        val restricted = finder(index, JvmClasspathRootId.of(inside))
        assertArrayEquals(byteArrayOf(2), restricted.findMetadata(classId("p/M"))!!.readBytes(), "the copy outside the classpath does not shadow it")
        assertArrayEquals(byteArrayOf(3), restricted.findMetadata(classId("p/N"))!!.readBytes())
        assertArrayEquals(byteArrayOf(1), finder(index, JvmClasspathRootId.of(outside)).findMetadata(classId("p/M"))!!.readBytes())
        assertNull(restricted.findMetadata(classId("p/Missing")))
    }

    // ---- 8. Multi-release jar ----

    @Test
    fun currentBehavior_multiReleaseVersionedEntriesAreNotFound() {
        val lib = jar(
            "mr.jar", "p/A.class", "META-INF/versions/11/p/A.class", "META-INF/versions/11/p/B.class",
            content = mapOf("META-INF/MANIFEST.MF" to "Manifest-Version: 1.0\nMulti-Release: true\n".toByteArray())
        )
        val index = createIndex(listOf(lib.jarRoot()))
        assertTrue(index.findClassFiles(classId("p/B"), CLASS).isEmpty())
        val a = index.findClassFiles(classId("p/A"), CLASS).single()
        assertEquals(lib.pathForVfs() + JAR_SEPARATOR + "p/A.class", a.pathForDiagnostics, "the root-level entry, not the versioned one")
        assertEquals(listOf("A"), index.classNames("p", CLASS))
    }

    // ---- 9. Case sensitivity ----

    @Test
    fun currentBehavior_jarRootLookupIsCaseSensitive() {
        val lib = jar("lib.jar", "p/A.class")
        val index = createIndex(listOf(lib.jarRoot()))
        assertTrue(index.findClassFiles(classId("p/a"), CLASS).isEmpty())
        assertFalse(index.hasPackage(fq("P")))
        assertEquals(1, index.findClassFiles(classId("p/A"), CLASS).size)
    }

    /**
     * Directory roots are case-sensitive as well, even on a case-insensitive file system (macOS and Windows by default): the core local
     * VFS matches child names exactly.
     */
    @Test
    fun currentBehavior_directoryRootLookupIsCaseSensitiveEvenOnCaseInsensitiveFileSystem() {
        val probe = dir("probe", "Probe.txt")
        val caseInsensitiveFileSystem = Files.exists(probe.resolve("pROBE.txt"))

        val bin = dir("bin", "p/A.class")
        val index = createIndex(listOf(bin.dirRoot()))
        val found = index.findClassFiles(classId("p/a"), CLASS)
        assertTrue(found.isEmpty(), "case-insensitive file system: $caseInsensitiveFileSystem, found: $found")
        assertFalse(index.hasPackage(fq("P")), "case-insensitive file system: $caseInsensitiveFileSystem")
        assertEquals(1, index.findClassFiles(classId("p/A"), CLASS).size)
    }

    // ---- 11. Filter during traversal (KT-91439) ----

    /**
     * The finder filters by classpath root while traversing the index (KT-91439): a library copy behind an incremental-compilation
     * output directory is found when only the library is on the classpath. Which copy wins when both are on it is KT-88892.
     */
    @Test
    fun filterDuringTraversal_outputDirDoesNotShadowLibraryCopy() {
        val out = dir("ic-out", "p/A.class")
        val lib = jar("lib.jar", "p/A.class")
        val index = createIndex(listOf(out.dirRoot(), lib.jarRoot()))
        val libraryId = JvmClasspathRootId.of(lib)
        val outId = JvmClasspathRootId.of(out)

        assertEquals(libraryId, finder(index, libraryId).findBinaryClassFile(classId("p/A"))?.rootId)
        val found = checkNotNull(finder(index, outId, libraryId).findBinaryClassFile(classId("p/A")))
        assertEquals("A", found.nameWithoutExtension)
        assertEquals(outId, found.rootId)
        assertEquals(outId, finder(index, outId).findBinaryClassFile(classId("p/A"))?.rootId)
    }

    /** HMPP-like setup: another fragment's root comes first, and the restricted fragment still sees its own copy. */
    @Test
    fun filterDuringTraversal_hmppOtherFragmentDoesNotShadowOwnCopy() {
        val other = dir("other", "p/A.class")
        val fragment = dir("fragment", "p/A.class")
        val fragmentId = JvmClasspathRootId.of(fragment)
        val index = createIndex(listOf(other.dirRoot(), fragment.dirRoot()))
        assertEquals(fragmentId, finder(index, fragmentId).findBinaryClassFile(classId("p/A"))?.rootId)
        assertEquals(fragmentId, finder(index, fragmentId).findSourceOrBinaryClassFile(classId("p/A"))?.rootId)
    }

    /** The project libraries exclude the incremental-compilation output: its copy is skipped and the library copy is found behind it. */
    @Test
    fun filterDuringTraversal_projectLibrariesSkipExcludedRoots() {
        val out = dir("ic-out", "p/A.class", "p/OnlyInOut.class")
        val lib = jar("lib.jar", "p/A.class")
        val index = createIndex(listOf(out.dirRoot(), lib.jarRoot()))
        val outId = JvmClasspathRootId.of(out)

        val libraries = finder(index, JvmClasspath.ProjectLibraries(listOf(outId)))
        assertEquals(JvmClasspathRootId.of(lib), libraries.findBinaryClassFile(classId("p/A"))?.rootId)
        assertNull(libraries.findBinaryClassFile(classId("p/OnlyInOut")))

        val allLibraries = finder(index, JvmClasspath.ProjectLibraries())
        assertEquals(outId, allLibraries.findBinaryClassFile(classId("p/A"))?.rootId, "nothing is excluded by default")
    }

    @Test
    fun filterDuringTraversal_binaryJavaClassCacheReturnsCopyFromAllowedRoot() {
        val first = dir("first", content = mapOf("p/A.class" to byteArrayOf(1)))
        val second = jar("second.jar", content = mapOf("p/A.class" to byteArrayOf(2)))
        val index = createIndex(listOf(first.dirRoot(), second.jarRoot()))
        val cache = BinaryJavaClassCache(CliBinaryClassFileIndex(index, enableSearchInCtSym = false))
        val firstId = JvmClasspathRootId.of(first)
        val secondId = JvmClasspathRootId.of(second)

        val allowed = cache.findTopLevelClassFiles(fq("p"), Name.identifier("A"), JvmClasspath.Roots(listOf(secondId))).single()
        assertEquals(secondId, allowed.rootId)
        assertArrayEquals(byteArrayOf(2), allowed.readBytes())

        assertEquals(firstId, cache.findTopLevelClassFiles(fq("p"), Name.identifier("A")).single().rootId, "unrestricted lookup")
        val noneAllowed = JvmClasspath.ProjectLibraries(listOf(firstId, secondId))
        assertTrue(cache.findTopLevelClassFiles(fq("p"), Name.identifier("A"), noneAllowed).isEmpty())
    }

    @Test
    fun currentBehavior_binaryClassFileIndexReturnsOnlyTheFirstMatch() {
        val first = dir("first", "p/A.class", "q/B.class")
        val second = jar("second.jar", "p/A.class", "p/C.class")
        val index = createIndex(listOf(first.dirRoot(), second.jarRoot()))
        val binaryIndex = CliBinaryClassFileIndex(index, enableSearchInCtSym = false)
        val files = binaryIndex.findTopLevelClassFiles(classId("p/A"))
        assertEquals(1, files.size)
        assertEquals(JvmClasspathRootId.of(first), files.single().rootId)
        assertEquals(setOf("A", "C"), binaryIndex.classFileNamesInPackage(fq("p")))
        assertTrue(binaryIndex.containsPackageDirectory(fq("q")))
        assertEquals(index.version, binaryIndex.classpathVersion)
    }

    // ---- 12. Handle semantics ----

    @Test
    fun handleOfDirectoryEntryKnowsItsRootAndSiblings() {
        val sub = dir("cp/out/sub", "p/A\$B.class", "p/A\$1.class", content = mapOf("p/A.class" to byteArrayOf(42)))
        val index = createIndex(listOf(sub.dirRoot()))
        val a = index.findClassFiles(classId("p/A"), CLASS).single()
        assertEquals(JvmClasspathRootId.of(sub), a.rootId)
        assertEquals(sub.toAbsolutePath().normalize(), a.rootPath)
        assertEquals(sub.pathForVfs() + "/p/A.class", a.pathForDiagnostics)
        assertArrayEquals(byteArrayOf(42), a.readBytes())

        val nested = checkNotNull(a.sibling("A\$B.class"))
        assertEquals("A\$B", nested.nameWithoutExtension)
        assertEquals(a.rootId, nested.rootId)
        assertEquals("A\$1", checkNotNull(a.sibling("A\$1.class")) { "a literal `\$` is kept in the name" }.nameWithoutExtension)
        assertNull(a.sibling("Missing.class"))

        assertTrue(a.isAcceptedBy(sub), "root equal to the classpath root")
        assertTrue(a.isAcceptedBy(tempDir.resolve("cp/out")), "directory root under a classpath directory")
        assertTrue(a.isAcceptedBy(tempDir.resolve("cp")))
        assertFalse(a.isAcceptedBy(tempDir.resolve("cp/out/su")), "a name prefix is not a parent directory")
        assertFalse(a.isAcceptedBy(tempDir.resolve("cp/out/sub/p")), "a directory inside the root")
    }

    @Test
    fun handleOfJarEntryKnowsItsRootAndSiblings() {
        val lib = jar("cp/lib.jar", "p/B.class", content = mapOf("p/A.class" to byteArrayOf(1, 2)))
        val index = createIndex(listOf(lib.jarRoot()))
        val a = index.findClassFiles(classId("p/A"), CLASS).single()
        assertEquals(lib.pathForVfs() + JAR_SEPARATOR + "p/A.class", a.pathForDiagnostics)
        assertEquals(JvmClasspathRootId.of(lib), a.rootId)
        assertEquals(lib.toAbsolutePath().normalize(), a.rootPath)
        assertArrayEquals(byteArrayOf(1, 2), a.readBytes())
        assertEquals(a.rootId, checkNotNull(a.sibling("B.class")).rootId)
        assertNull(a.sibling("C.class"))

        assertTrue(a.isAcceptedBy(lib), "jar root equal to the classpath root")
        assertFalse(a.isAcceptedBy(tempDir.resolve("cp")), "jar inside a classpath directory")
    }

    private fun BinaryClassFileHandle.isAcceptedBy(classpathRoot: Path): Boolean =
        JvmClasspathRootFilter(JvmClasspath.Roots(listOf(JvmClasspathRootId.of(classpathRoot)))).accepts(this)
}

class JvmDependenciesIndexImplContractTest : AbstractJvmDependenciesIndexContractTest() {
    override fun createIndex(roots: List<JavaRoot>): JvmDependenciesIndex = JvmDependenciesIndexImpl(roots, rootRegistry.vfsEntries)
}

/** The same contract holds for a [JvmDependenciesIndexImpl] whose roots have NIO entries ([NioRootEntryFactory]). */
class JvmDependenciesIndexNioEntriesContractTest : AbstractJvmDependenciesIndexContractTest() {
    private val jarPool = checkNotNull(JarFilePool.createIfUnmappingPossible())

    @AfterEach
    fun closeJarPool() = jarPool.close()

    override fun createIndex(roots: List<JavaRoot>): JvmDependenciesIndex =
        JvmDependenciesIndexImpl(roots, NioRootEntryFactory(jarPool, rootRegistry))
}

/** The same contract holds for a first-only [JvmDependenciesDynamicCompoundIndex] wrapping a single [JvmDependenciesIndexImpl]. */
class JvmDependenciesIndexOverDynamicCompoundContractTest : AbstractJvmDependenciesIndexContractTest() {
    override fun createIndex(roots: List<JavaRoot>): JvmDependenciesIndex =
        JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true).apply { addIndex(JvmDependenciesIndexImpl(roots, rootRegistry.vfsEntries)) }
}

/** Behavior specific to [JvmDependenciesDynamicCompoundIndex] composed of several sub-indices. */
class JvmDependenciesIndexDynamicCompoundContractTest : JvmDependenciesIndexContractTestBase() {
    @Test
    fun versionIncrementsOnEveryAddIndex() {
        val compound = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true)
        assertEquals(0, compound.version)
        compound.addIndex(JvmDependenciesIndexImpl(listOf(dir("a").dirRoot()), rootRegistry.vfsEntries))
        assertEquals(1, compound.version)
        compound.addIndex(JvmDependenciesIndexImpl(emptyList(), rootRegistry.vfsEntries))
        assertEquals(2, compound.version)
        assertEquals(0, JvmDependenciesIndexImpl(emptyList(), rootRegistry.vfsEntries).version)
        assertEquals(2, CliBinaryClassFileIndex(compound, enableSearchInCtSym = false).classpathVersion)
    }

    @Test
    fun resultsOfLaterAddedIndicesAppearAfterAdding() {
        val first = dir("first", "p/A.class")
        val second = jar("second.jar", "p/B.class", "q/C.class", content = mapOf("q/q.kotlin_builtins" to byteArrayOf(5)))
        val compound = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true)
        compound.addIndex(JvmDependenciesIndexImpl(listOf(first.dirRoot()), rootRegistry.vfsEntries))
        assertTrue(compound.findClassFiles(classId("p/B"), CLASS).isEmpty())
        assertFalse(compound.hasPackage(fq("q")))
        assertNull(compound.findBuiltInsFile(fq("q")))

        compound.addIndex(JvmDependenciesIndexImpl(listOf(second.jarRoot()), rootRegistry.vfsEntries))
        assertEquals(JvmClasspathRootId.of(second), compound.findClassFiles(classId("p/B"), CLASS).single().rootId)
        assertTrue(compound.hasPackage(fq("q")))
        assertArrayEquals(byteArrayOf(5), compound.findBuiltInsFile(fq("q"))!!.readBytes())
        assertEquals(listOf("A", "B"), compound.classNames("p", CLASS))
        assertEquals(listOf(JvmClasspathRootId.of(first), JvmClasspathRootId.of(second)), compound.indexedRoots.map { it.id }.toList())
    }

    @Test
    fun firstOnlyModeReturnsTheFirstSubIndexMatch() {
        val compound = compoundOfTwoWithClassA(shouldOnlyFindFirstClass = true)
        assertEquals(JvmClasspathRootId.of(tempDir.resolve("first")), compound.findClassFiles(classId("p/A"), CLASS).single().rootId)
    }

    @Test
    fun allModeReturnsOneMatchPerSubIndexInOrder() {
        val compound = compoundOfTwoWithClassA(shouldOnlyFindFirstClass = false)
        assertEquals(
            listOf(JvmClasspathRootId.of(tempDir.resolve("first")), JvmClasspathRootId.of(tempDir.resolve("second.jar"))),
            compound.findClassFiles(classId("p/A"), CLASS).map { it.rootId }
        )
        assertTrue(compound.findClassFiles(classId("p/Missing"), CLASS).isEmpty())
    }

    @Test
    fun allModeStillReturnsOnlyFirstMatchWithinOneSubIndex() {
        val first = dir("first", "p/A.class")
        val second = dir("second", "p/A.class")
        val compound = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = false)
        compound.addIndex(JvmDependenciesIndexImpl(listOf(first.dirRoot(), second.dirRoot()), rootRegistry.vfsEntries))
        assertEquals(1, compound.findClassFiles(classId("p/A"), CLASS).size)
    }

    @Test
    fun getUnindexedRootsReturnsRootsNotInAnySubIndex() {
        val a = dir("a").dirRoot()
        val b = jar("b.jar", "x/X.class").jarRoot()
        val c = dir("c").dirRoot()
        val compound = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true)
        assertEquals(listOf(a, b), compound.getUnindexedRoots(listOf(a, b)))
        compound.addIndex(JvmDependenciesIndexImpl(listOf(a), rootRegistry.vfsEntries))
        compound.addIndex(JvmDependenciesIndexImpl(listOf(b), rootRegistry.vfsEntries))
        assertEquals(listOf(c), compound.getUnindexedRoots(listOf(a, b, c)))
        assertEquals(
            listOf(rootRegistry.javaRoot(rootRegistry.virtualFile(a), JavaRoot.RootType.SOURCE)),
            compound.getUnindexedRoots(listOf(rootRegistry.javaRoot(rootRegistry.virtualFile(a), JavaRoot.RootType.SOURCE))),
            "roots are compared as a whole, including their type"
        )
    }

    @Test
    fun getUnindexedRootsComparesRootIds() {
        val a = dir("a")
        val lib = jar("lib.jar", "p/A.class")
        val indexedDir = a.dirRoot()
        val indexedJar = lib.jarRoot()
        val compound = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true)
        compound.addIndex(JvmDependenciesIndexImpl(listOf(indexedDir, indexedJar), rootRegistry.vfsEntries))

        val sameDirectoryAgain = a.dirRoot()
        val sameJarAgain = lib.jarRoot()
        assertEquals(indexedDir.id, sameDirectoryAgain.id)
        assertEquals(indexedJar.id, sameJarAgain.id)
        // Roots are identities: a root resolved again (e.g. by a classpath update) is indexed already, even with another `VirtualFile`.
        assertEquals(emptyList<JavaRoot>(), compound.getUnindexedRoots(listOf(sameDirectoryAgain, sameJarAgain)))
    }

    @Test
    fun traverseStopsAcrossSubIndicesWhenCallbackReturnsFalse() {
        val compound = compoundOfTwoWithClassA(shouldOnlyFindFirstClass = true)
        val seen = mutableListOf<BinaryClassFileHandle>()
        compound.traverseFilesInPackage(fq("p"), CLASS) { seen.add(it); false }
        assertEquals(listOf("A"), seen.map { it.nameWithoutExtension })

        val all = mutableListOf<BinaryClassFileHandle>()
        compound.traverseFilesInPackage(fq("p"), CLASS) { all.add(it); true }
        assertEquals(listOf("A", "A"), all.map { it.nameWithoutExtension })
        assertEquals(2, all.map { it.rootId }.distinct().size)
        assertEquals(all.first().rootId, seen.single().rootId)
    }

    private fun compoundOfTwoWithClassA(shouldOnlyFindFirstClass: Boolean): JvmDependenciesDynamicCompoundIndex {
        val first = dir("first", "p/A.class")
        val second = jar("second.jar", "p/A.class")
        return JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass).apply {
            addIndex(JvmDependenciesIndexImpl(listOf(first.dirRoot()), rootRegistry.vfsEntries))
            addIndex(JvmDependenciesIndexImpl(listOf(second.jarRoot()), rootRegistry.vfsEntries))
        }
    }
}

class JvmDependenciesIndexRootIdContractTest : JvmDependenciesIndexContractTestBase() {
    @Test
    fun rootIdOfPathIsAbsoluteNormalizedSlashSeparatedWithoutTrailingSlash() {
        val base = tempDir.toAbsolutePath().normalize().toString().replace('\\', '/')
        assertEquals(JvmClasspathRootId("$base/b"), JvmClasspathRootId.of(tempDir.resolve("a/../b")))
        assertEquals(JvmClasspathRootId("$base/a/b"), JvmClasspathRootId.of(tempDir.resolve("a/./b/")))
        assertEquals(JvmClasspathRootId("$base/x.jar"), JvmClasspathRootId.of(tempDir.resolve("x.jar")))
        assertFalse(JvmClasspathRootId.of(tempDir.resolve("a/")).id.endsWith("/"))

        val relative = JvmClasspathRootId.of(Paths.get("some/relative/../dir"))
        assertEquals(JvmClasspathRootId.of(Paths.get("").toAbsolutePath().resolve("some/dir")), relative)
        assertTrue(Paths.get(relative.id).isAbsolute)
    }

    @Test
    fun currentBehavior_rootIdOfFileSystemRootIsEmpty() {
        val fsRoot = tempDir.toAbsolutePath().root
        assertEquals(JvmClasspathRootId(fsRoot.toString().replace('\\', '/').trimEnd('/')), JvmClasspathRootId.of(fsRoot))
    }

    @Test
    fun rootIdOfVirtualFileMatchesRootIdOfPath() {
        val dir = dir("lib/classes", "p/A.class")
        val jar = jar("lib/lib.jar", "p/A.class")

        val dirFile = dir.asLocalVirtualFile()
        assertEquals(JvmClasspathRootId.of(dir), dirFile.asJvmClasspathRootId())
        assertFalse(dirFile.asJvmClasspathRootId().id.endsWith("/"))

        val jarFile = jar.asJarRootVirtualFile()
        assertTrue(jarFile.path.endsWith(JAR_SEPARATOR), jarFile.path)
        assertEquals(JvmClasspathRootId.of(jar), jarFile.asJvmClasspathRootId(), "`!/` is removed")

        assertEquals(JvmClasspathRootId.of(tempDir.resolve("lib/x/../classes")), dirFile.asJvmClasspathRootId(), "`..` is normalized")
        assertEquals(rootRegistry.javaRoot(dirFile, JavaRoot.RootType.BINARY).id, JvmClasspathRootId.of(dir))
        assertEquals(rootRegistry.javaRoot(jarFile, JavaRoot.RootType.BINARY).id, JvmClasspathRootId.of(jar))
    }
}
