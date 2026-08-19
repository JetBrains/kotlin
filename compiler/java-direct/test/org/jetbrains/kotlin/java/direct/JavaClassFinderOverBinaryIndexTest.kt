/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileIndex
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClassCache
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.asBinaryClassFileHandle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.compileJavaFiles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

/**
 * What the classpath restriction of [JavaClassFinderOverBinaryIndex] decides, as opposed to
 * [ClasspathRestrictionTest], which pins the `isUnder` predicate it is built on.
 *
 * The classpath here is the one of an incremental compilation — the only shape in which a session is given a
 * proper part of the classpath of the compilation, and therefore the only one in which the two lookups of the
 * finder can disagree. The output directory of the previous build (`out/`) holds a `p.A` of its own, the
 * libraries hold another `p.A` and the `p.Lib` that `out/p/Ref.class` mentions in a signature:
 *
 *   * the precompiled-binaries session is `Roots([out])`, and does not contain the libraries `Ref` was compiled
 *     against;
 *   * the libraries session is `ProjectLibraries(excludedRoots = [out])`, and must not see the output directory
 *     a second time — the class files there are the previous version of what this compilation is producing.
 *
 * Both sessions share one index and get the same list of candidates for `p.A`: only the part of the classpath a
 * lookup may see differs, and that alone decides which `p.A` is read, or whether one is read at all.
 */
class JavaClassFinderOverBinaryIndexTest {

    @Test
    fun testTheVisibleClasspathDecidesWhichCopyOfAClassIsRead(@TempDir tempDir: Path) {
        val fixture = TwoRootsFixture(tempDir)

        assertEquals(
            OUTPUT_DIRECTORY_MARKER, fixture.finder(fixture.previousOutput).fieldOfA(),
            "the precompiled-binaries session reads the class file of the previous build"
        )
        assertEquals(
            LIBRARY_MARKER, fixture.finder(fixture.librariesWithoutPreviousOutput).fieldOfA(),
            "the libraries session skips the excluded output directory, even though it comes first among the candidates"
        )
        assertEquals(
            OUTPUT_DIRECTORY_MARKER, fixture.finder(JvmClasspath.ProjectLibraries()).fieldOfA(),
            "with nothing excluded the first candidate wins, which is what both lookups above would do unfiltered"
        )
    }

    @Test
    fun testAClassOutsideTheVisibleClasspathIsNotFound(@TempDir tempDir: Path) {
        val fixture = TwoRootsFixture(tempDir)

        assertNull(
            fixture.finder(fixture.previousOutput).findClass(LIB_ID),
            "the libraries are not among the roots of the precompiled-binaries session"
        )
        assertNull(
            fixture.finder(fixture.previousOutput).findClass(NESTED_ID),
            "and neither is the class nested in one of them"
        )
        assertNotNull(
            fixture.finder(fixture.librariesWithoutPreviousOutput).findClass(LIB_ID),
            "excluding the output directory does not hide the libraries"
        )
    }

    /**
     * The contrast to [testAClassOutsideTheVisibleClasspathIsNotFound]: the very class this session may not be
     * asked about is still resolved when the bytecode of a class it does read refers to it. A reference recorded
     * in a class file is bound to the classpath that class file was compiled against, which is wider than the
     * part of the classpath its reader may see, so it is looked up on the whole classpath (KT-17897).
     *
     * Were it looked up in the visible classpath instead, the return type of `Ref.get()` would silently become
     * an unresolved `p.Lib.Nested`.
     */
    @Test
    fun testACrossReferenceIsResolvedOutsideTheVisibleClasspath(@TempDir tempDir: Path) {
        val finder = TwoRootsFixture(tempDir).let { it.finder(it.previousOutput) }

        val referenced = finder.returnTypeOfRefGet()
        assertEquals(
            NESTED_ID.asSingleFqName(), (referenced.classifier as? JavaClass)?.fqName,
            "the reference is resolved to the class file in the library, which `findClass` of this session does not reach"
        )
    }

    private fun JavaClassFinderOverBinaryIndex.fieldOfA(): String =
        checkNotNull(findClass(A_ID)) { "p.A is on every classpath of the fixture" }
            .fields.single().name.asString()

    private fun JavaClassFinderOverBinaryIndex.returnTypeOfRefGet(): JavaClassifierType {
        val ref = checkNotNull(findClass(REF_ID)) { "p.Ref lies in the output directory of the previous build" }
        return ref.methods.single { it.name.asString() == "get" }.returnType as JavaClassifierType
    }

    /**
     * The binary classpath of one incremental compilation: the output directory of the previous build and one
     * library jar, both declaring `p.A`, the output directory first as it comes first on the command line.
     *
     * The class files are real — they are compiled by `javac` and read from disk — but they are then handed to
     * the index as [InMemoryClassFileRoot]s, which is both what keeps the test independent of a file system and
     * a root shape the compiler has to support anyway (a build system may compile against an output of its own
     * that was never written to disk).
     */
    private class TwoRootsFixture(tempDir: Path) {
        private val libraryClasses: Path = tempDir.resolve("lib").compileJava(
            "p/A.java" to "package p; public class A { public int $LIBRARY_MARKER; }",
            "p/Lib.java" to "package p; public class Lib { public static class Nested {} }",
        )

        private val outputClasses: Path = tempDir.resolve("out").compileJava(
            "p/A.java" to "package p; public class A { public int $OUTPUT_DIRECTORY_MARKER; }",
            "p/Ref.java" to "package p; public class Ref { public Lib.Nested get() { return null; } }",
            classpath = libraryClasses,
        )

        private val outputRoot = InMemoryClassFileRoot(OUTPUT_ROOT_PATH, isArchive = false).apply {
            add("p/A.class", outputClasses)
            add("p/Ref.class", outputClasses)
        }

        private val libraryRoot = InMemoryClassFileRoot(LIBRARY_ROOT_PATH, isArchive = true).apply {
            add("p/A.class", libraryClasses)
            add("p/Lib.class", libraryClasses)
            add($$"p/Lib$Nested.class", libraryClasses)
        }

        private val index = object : BinaryClassFileIndex {
            private val classFiles: Map<ClassId, List<BinaryClassFileHandle>> = mapOf(
                A_ID to listOf(outputRoot.handle("p/A.class"), libraryRoot.handle("p/A.class")),
                REF_ID to listOf(outputRoot.handle("p/Ref.class")),
                LIB_ID to listOf(libraryRoot.handle("p/Lib.class")),
            )

            override fun findTopLevelClassFiles(topLevelClassId: ClassId): Collection<BinaryClassFileHandle> =
                classFiles[topLevelClassId].orEmpty()

            override fun classFileNamesInPackage(packageFqName: FqName): Set<String> =
                classFiles.keys.filterTo(HashSet()) { it.packageFqName == packageFqName }.mapTo(HashSet()) {
                    it.shortClassName.asString()
                }

            override fun containsPackageDirectory(packageFqName: FqName): Boolean =
                classFiles.keys.any { it.packageFqName == packageFqName }
        }

        val previousOutput: JvmClasspath
            get() = JvmClasspath.Roots(listOf(JvmClasspathRootId(OUTPUT_ROOT_PATH)))

        val librariesWithoutPreviousOutput: JvmClasspath
            get() = JvmClasspath.ProjectLibraries(excludedRoots = listOf(JvmClasspathRootId(OUTPUT_ROOT_PATH)))

        fun finder(classpath: JvmClasspath): JavaClassFinderOverBinaryIndex =
            JavaClassFinderOverBinaryIndex(BinaryJavaClassCache(index), classpath)

        private fun Path.compileJava(vararg sources: Pair<String, String>, classpath: Path? = null): Path = apply {
            val sourceDir: Path = resolveSibling(fileName.toString() + "-src")
            val files = ArrayList<File>()
            for (source in sources) {
                val sourceFile: Path = sourceDir.resolve(source.first)
                sourceFile.parent.createDirectories()
                sourceFile.writeText(source.second)
                files.add(sourceFile.toFile())
            }
            createDirectories()
            val options = listOfNotNull("-d", toString(), classpath?.let { "-classpath" }, classpath?.toString())
            compileJavaFiles(files, options).assertSuccessful()
        }
    }

    /**
     * One classpath root of class files held in memory, spelling its paths as the VFS does: entries of an
     * archive are named after the archive itself, so that `BinaryClassFileHandle.isUnder` sees the shape it
     * would see in a compilation.
     */
    private class InMemoryClassFileRoot(rootPath: String, isArchive: Boolean) {
        private val directories = HashMap<String, InMemoryVirtualFile>()

        private val root = InMemoryVirtualFile(rootPath + if (isArchive) JAR_SEPARATOR else "", "", null, null)

        fun add(relativePath: String, classesDirectory: Path) {
            val content = classesDirectory.resolve(relativePath).readBytes()
            val directory = directoryFor(relativePath.substringBeforeLast('/', missingDelimiterValue = ""))
            val name = relativePath.substringAfterLast('/')
            InMemoryVirtualFile(directory.path.removeSuffix("/") + "/" + name, name, directory, content)
        }

        fun handle(relativePath: String): BinaryClassFileHandle =
            checkNotNull(root.findFileByRelativePath(relativePath)) { "no $relativePath in $root" }.asBinaryClassFileHandle()

        private fun directoryFor(relativePath: String): InMemoryVirtualFile {
            if (relativePath.isEmpty()) return root
            return directories.getOrPut(relativePath) {
                val parent = directoryFor(relativePath.substringBeforeLast('/', missingDelimiterValue = ""))
                val name = relativePath.substringAfterLast('/')
                InMemoryVirtualFile(parent.path.removeSuffix("/") + "/" + name, name, parent, null)
            }
        }
    }

    /** A class file, or a directory of them, with a path but no location: nothing here touches a file system. */
    private class InMemoryVirtualFile(
        private val filePath: String,
        private val fileName: String,
        private val parentFile: InMemoryVirtualFile?,
        private val content: ByteArray?,
    ) : VirtualFile() {
        private val childFiles = ArrayList<VirtualFile>()

        init {
            parentFile?.childFiles?.add(this)
        }

        override fun getName(): String = fileName
        override fun getPath(): String = filePath
        override fun getFileSystem(): VirtualFileSystem = CoreLocalFileSystem()
        override fun isWritable(): Boolean = false
        override fun isDirectory(): Boolean = content == null
        override fun isValid(): Boolean = true
        override fun getParent(): VirtualFile? = parentFile
        override fun getChildren(): Array<VirtualFile> = childFiles.toTypedArray()
        override fun contentsToByteArray(): ByteArray = checkNotNull(content) { "$filePath is a directory" }
        override fun getInputStream(): InputStream = ByteArrayInputStream(contentsToByteArray())
        override fun getTimeStamp(): Long = 0
        override fun getModificationStamp(): Long = 0
        override fun getLength(): Long = content?.size?.toLong() ?: 0
        override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

        override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
            throw UnsupportedOperationException("read-only")

        override fun toString(): String = filePath
    }

    private companion object {
        /** The name of the field of `p.A`, telling apart the two class files declaring it. */
        private const val OUTPUT_DIRECTORY_MARKER = "fromTheOutputDirectory"
        private const val LIBRARY_MARKER = "fromTheLibrary"

        private const val OUTPUT_ROOT_PATH = "/previous-build/out"
        private const val LIBRARY_ROOT_PATH = "/libraries/lib.jar"

        private val A_ID = ClassId(FqName("p"), Name.identifier("A"))
        private val REF_ID = ClassId(FqName("p"), Name.identifier("Ref"))
        private val LIB_ID = ClassId(FqName("p"), Name.identifier("Lib"))
        private val NESTED_ID = ClassId.fromString("p/Lib.Nested")
    }
}
