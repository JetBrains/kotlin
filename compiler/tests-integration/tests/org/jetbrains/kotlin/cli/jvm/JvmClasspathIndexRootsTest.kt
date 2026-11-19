/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.psi.impl.file.impl.JavaFileManager
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.testEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.index.VfsClassFileHandle
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.useNioJvmDependenciesIndex
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.disposeRootDisposable
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

/**
 * Parity contract for a new implementation of the CLI classpath index.
 */
open class JvmClasspathIndexRootsTest {
    protected val testRootDisposable: Disposable = Disposer.newDisposable()

    protected class IndexedEnvironment(
        val index: JvmDependenciesIndex,
        val javaFileManager: KotlinCliJavaFileManagerImpl,
        val updateClasspath: (List<File>) -> Unit,
    )

    /** Whether the class finder of the environment is expected to have NIO root entries (`-Xuse-nio-jvm-dependencies-index`). */
    protected open val nioEntries: Boolean get() = false

    protected fun CompilerConfiguration.withBackend(): CompilerConfiguration = apply { useNioJvmDependenciesIndex = nioEntries }

    protected fun assertBackend(handle: BinaryClassFileHandle) {
        assertEquals(!nioEntries, handle is VfsClassFileHandle, "the backend of $handle")
    }

    protected open fun createIndexedEnvironment(configuration: CompilerConfiguration): IndexedEnvironment {
        @OptIn(CoreEnvironmentDeprecation::class)
        val environment =
            KotlinCoreEnvironment.create(testRootDisposable, configuration.withBackend())
        val project = environment.project
        return IndexedEnvironment(
            (project.getService(VirtualFileFinderFactory::class.java) as CliVirtualFileFinderFactory).index,
            project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl,
        ) { files -> environment.updateClasspath(files.map { JvmClasspathRoot(it) }) }
    }

    @AfterEach
    fun tearDown() {
        disposeRootDisposable(testRootDisposable)
        // Opened archives are cached process-wide, which would keep `TempDir` from deleting them on Windows.
        ZipHandler.clearFileAccessorCache()
    }

    /** Java source roots come first, then classpath entries in their order, then the module roots of a modular JDK. */
    @Test
    fun testRootOrderIsSourcesThenClasspathThenModularJdk(@TempDir tempDir: Path) {
        assumeModularTestJdk()
        val sources = tempDir.resolve("src").createDirectories()
        val classes1 = tempDir.resolve("classes1").createDirectories()
        val jar2 = tempDir.resolve("lib/lib2.jar").createParentsAndWriteJar("p/C2.class")
        val classes3 = tempDir.resolve("classes3").createDirectories()

        val configuration = configurationWithJdk(currentJdkHome()).apply {
            addJvmClasspathRoot(classes1.toFile())
            addJavaSourceRoot(sources.toFile())
            addJvmClasspathRoot(jar2.toFile())
            addJvmClasspathRoot(classes3.toFile())
            configureJdkClasspathRoots()
        }
        val index = createIndexedEnvironment(configuration).index
        val roots = index.indexedRoots.toList()

        assertEquals(
            listOf(
                sources.localPath() to JavaRoot.RootType.SOURCE,
                classes1.localPath() to JavaRoot.RootType.BINARY,
                jar2.localPath() to JavaRoot.RootType.BINARY,
                classes3.localPath() to JavaRoot.RootType.BINARY,
            ),
            roots.take(4).map { it.localPath() to it.type },
        )
        assertEquals(null, roots[0].prefixFqName)

        val jdkRoots = roots.drop(4)
        assertTrue(jdkRoots.isNotEmpty(), "the modules of the JDK are expected after the classpath: $roots")
        assertTrue(jdkRoots.all { it.isJrtRoot() && it.type == JavaRoot.RootType.BINARY }, "all JDK roots are jrt BINARY roots: $jdkRoots")
        assertEquals(jdkRoots.size, jdkRoots.mapTo(HashSet()) { it.file.path }.size, "every JDK module has its own root")
        val javaBase = jdkRoots.single { it.file.path.endsWith("/modules/java.base") }
        val objectClass = index.findClassFiles(ClassId.fromString("java/lang/Object"), CLASS_FILES).single()
        assertEquals(javaBase.id, objectClass.rootId)
        assertBackend(objectClass)
    }

    @Test
    fun testJdk8ClassRootsFollowJavaSdkUtilOrderBeforeUserClasspath(@TempDir tempDir: Path) {
        val jdk8Home = findJdk8Home()
        assumeTrue(jdk8Home != null, "no JDK 8 available")
        jdk8Home!!
        val sources = tempDir.resolve("src").createDirectories()
        val classes = tempDir.resolve("classes").createDirectories()

        val configuration = configurationWithJdk(jdk8Home).apply {
            addJavaSourceRoot(sources.toFile())
            addJvmClasspathRoot(classes.toFile())
            configureJdkClasspathRoots()
        }
        val index = createIndexedEnvironment(configuration).index
        val roots = index.indexedRoots.toList()

        val expectedJdkRoots = PathUtil.getJdkClassesRootsFromJdkOrJre(jdk8Home).map { it.absolutePath }
        assertTrue(expectedJdkRoots.any { it.endsWith("/jre/lib/rt.jar") }, "rt.jar is a JDK 8 class root: $expectedJdkRoots")
        assertTrue(expectedJdkRoots.any { "/jre/lib/ext/" in it }, "jre/lib/ext jars are JDK 8 class roots: $expectedJdkRoots")

        assertEquals(
            listOf(sources.localPath() to JavaRoot.RootType.SOURCE) +
                    expectedJdkRoots.map { it to JavaRoot.RootType.BINARY } +
                    listOf(classes.localPath() to JavaRoot.RootType.BINARY),
            roots.map { it.localPath() to it.type },
        )
        assertTrue(roots.none { it.isJrtRoot() }, "a JDK 8 home has no module roots: $roots")

        val objectClass = index.findClassFiles(ClassId.fromString("java/lang/Object"), CLASS_FILES).single()
        assertTrue(objectClass.rootId.id.endsWith("/jre/lib/rt.jar"), "java.lang.Object comes from rt.jar: ${objectClass.rootId}")
        assertBackend(objectClass)
    }

    @Test
    fun testNoJdkAddsNoJdkRoots(@TempDir tempDir: Path) {
        val classes = tempDir.resolve("classes").createDirectories()

        val configuration = noJdkConfiguration().apply {
            addJvmClasspathRoot(classes.toFile())
            configureJdkClasspathRoots()
        }
        val index = createIndexedEnvironment(configuration).index

        assertEquals(listOf(classes.localPath() to JavaRoot.RootType.BINARY), index.indexedRoots.map { it.localPath() to it.type }.toList())
        assertTrue(index.findClassFiles(ClassId.fromString("java/lang/Object"), CLASS_AND_SIG_FILES).isEmpty())
    }

    @Test
    fun currentBehavior_noJdkWithModularJdkHomeStillIndexesJrtModules(@TempDir tempDir: Path) {
        assumeModularTestJdk()
        val classes = tempDir.resolve("classes").createDirectories()

        val configuration = configurationWithJdk(currentJdkHome()).apply {
            put(JVMConfigurationKeys.NO_JDK, true)
            addJvmClasspathRoot(classes.toFile())
            configureJdkClasspathRoots()
        }
        val roots = createIndexedEnvironment(configuration).index.indexedRoots.toList()

        assertEquals(listOf(classes.localPath() to JavaRoot.RootType.BINARY), roots.take(1).map { it.localPath() to it.type })
        val jdkRoots = roots.drop(1)
        assertTrue(jdkRoots.isNotEmpty(), "module roots are indexed despite NO_JDK: $roots")
        assertTrue(jdkRoots.all { it.isJrtRoot() && it.type == JavaRoot.RootType.BINARY }, "all of them are jrt BINARY roots: $jdkRoots")
    }

    @Test
    fun testJdkRelease8IndexesSingleCtSymRoot(@TempDir tempDir: Path) {
        assumeModularTestJdk()
        val classes = tempDir.resolve("classes").createDirectories()

        val configuration = configurationWithJdk(currentJdkHome()).apply {
            put(JVMConfigurationKeys.JDK_RELEASE, 8)
            addJvmClasspathRoot(classes.toFile())
            configureJdkClasspathRoots()
        }
        val index = createIndexedEnvironment(configuration).index
        val roots = index.indexedRoots.toList()

        assertEquals(listOf(classes.localPath() to JavaRoot.RootType.BINARY), roots.take(1).map { it.localPath() to it.type })
        val ctSymRoot = roots.drop(1).single()
        assertEquals(JavaRoot.RootType.BINARY_SIG, ctSymRoot.type)
        assertTrue("ct.sym" in ctSymRoot.file.path, "the release root lies in ct.sym: ${ctSymRoot.file.path}")
        assertTrue(roots.none { it.isJrtRoot() }, "no jrt roots with -Xjdk-release: $roots")

        val objectClass = index.findClassFiles(ClassId.fromString("java/lang/Object"), SIG_FILES).single()
        assertEquals(ctSymRoot.id, objectClass.rootId)
        assertBackend(objectClass)
        assertEquals("sig", objectClass.extension)
    }

    @Test
    fun testJdkRelease11IndexesOneCtSymRootPerModule(@TempDir tempDir: Path) {
        assumeModularTestJdk()
        val classes = tempDir.resolve("classes").createDirectories()

        val configuration = configurationWithJdk(currentJdkHome()).apply {
            put(JVMConfigurationKeys.JDK_RELEASE, 11)
            addJvmClasspathRoot(classes.toFile())
            configureJdkClasspathRoots()
        }
        val index = createIndexedEnvironment(configuration).index
        val roots = index.indexedRoots.toList()

        assertEquals(listOf(classes.localPath() to JavaRoot.RootType.BINARY), roots.take(1).map { it.localPath() to it.type })
        val jdkRoots = roots.drop(1)
        assertTrue(jdkRoots.size > 1, "several JDK modules are expected: $jdkRoots")
        assertTrue(jdkRoots.all { it.type == JavaRoot.RootType.BINARY_SIG }, "all JDK roots are BINARY_SIG: $jdkRoots")
        assertEquals(jdkRoots.size, jdkRoots.mapTo(HashSet()) { it.id }.size, "every ct.sym root has its own id")
        assertEquals(1, jdkRoots.mapTo(HashSet()) { it.file.path }.size, "...although they all share one path")
        assertTrue(roots.none { it.isJrtRoot() }, "no jrt roots with -Xjdk-release: $roots")

        val objectClass = index.findClassFiles(ClassId.fromString("java/lang/Object"), SIG_FILES).single()
        assertEquals(jdkRoots.single { it.id.id.endsWith("/java.base") }.id, objectClass.rootId)
        assertBackend(objectClass)
        assertEquals("sig", objectClass.extension)
    }

    @Test
    fun testSingleJavaFileRootIsNotIndexedButFoundByJavaFileManager(@TempDir tempDir: Path) {
        val javaFile = tempDir.resolve("src/pkg/Single.java").createParentsAndWriteText("package pkg;\n\npublic class Single {}\n")
        val classes = tempDir.resolve("classes").createDirectories()

        val configuration = configurationWithJdk(currentJdkHome()).apply {
            addJavaSourceRoot(javaFile.toFile())
            addJvmClasspathRoot(classes.toFile())
            configureJdkClasspathRoots()
        }
        val environment = createIndexedEnvironment(configuration)
        val roots = environment.index.indexedRoots.toList()

        assertTrue(roots.none { it.type == JavaRoot.RootType.SOURCE }, "no source root is indexed: $roots")
        val sourceDir = javaFile.parent.parent.localPath()
        assertTrue(roots.none { it.localPath() == sourceDir || it.localPath().startsWith("$sourceDir/") }, "nothing under $sourceDir: $roots")
        assertEquals(classes.localPath(), roots.first().localPath())

        val classId = ClassId.fromString("pkg/Single")
        assertTrue(environment.index.findClassFiles(classId, JAVA_FILES).isEmpty(), "the main index does not know the file")
        assertNotNull(environment.javaFileManager.findClassOnClasspath(classId), "the Java file manager finds the class of the single file")
    }

    @Test
    fun testJavaSourceRootWithPackagePrefix(@TempDir tempDir: Path) {
        val prefixed = tempDir.resolve("prefixed").createDirectories()
        prefixed.resolve("Foo.java").writeText("package my.pkg;\n\npublic class Foo {}\n")
        val plain = tempDir.resolve("plain").createDirectories()

        val configuration = noJdkConfiguration().apply {
            addJavaSourceRoot(prefixed.toFile(), "my.pkg")
            addJavaSourceRoot(plain.toFile())
        }
        val environment = createIndexedEnvironment(configuration)
        val roots = environment.index.indexedRoots.toList()

        assertEquals(
            listOf(
                Triple(prefixed.localPath(), JavaRoot.RootType.SOURCE, FqName("my.pkg")),
                Triple(plain.localPath(), JavaRoot.RootType.SOURCE, null),
            ),
            roots.map { Triple(it.localPath(), it.type, it.prefixFqName) },
        )

        val foo = environment.index.findClassFiles(ClassId.fromString("my/pkg/Foo"), JAVA_FILES).single()
        assertEquals(roots[0].id, foo.rootId)
        assertBackend(foo)
    }

    @Test
    fun testUpdateClasspathAppendsOnlyNewRoots(@TempDir tempDir: Path) {
        val classesA = tempDir.resolve("a").createDirectories()
        classesA.resolve("pa/A.class").createParentsAndWrite()
        val classesB = tempDir.resolve("b").createDirectories()
        classesB.resolve("pb/B.class").createParentsAndWrite()
        val jarC = tempDir.resolve("lib/c.jar").createParentsAndWriteJar("pc/C.class")

        val configuration = configurationWithJdk(currentJdkHome()).apply {
            addJvmClasspathRoot(classesA.toFile())
            configureJdkClasspathRoots()
        }
        val environment = createIndexedEnvironment(configuration)
        val index = environment.index
        val rootsBefore = index.indexedRoots.toList()
        val versionBefore = index.version
        assertTrue(index.findClassFiles(ClassId.fromString("pb/B"), CLASS_FILES).isEmpty())

        environment.updateClasspath(listOf(classesA.toFile(), classesB.toFile(), jarC.toFile()))

        val rootsAfter = index.indexedRoots.toList()
        assertTrue(index.version > versionBefore, "the version increases: $versionBefore -> ${index.version}")
        assertEquals(rootsBefore, rootsAfter.take(rootsBefore.size), "the existing roots keep their order")
        assertEquals(
            listOf(classesB.localPath() to JavaRoot.RootType.BINARY, jarC.localPath() to JavaRoot.RootType.BINARY),
            rootsAfter.drop(rootsBefore.size).map { it.localPath() to it.type },
        )
        assertEquals(rootsAfter.size, rootsAfter.toSet().size, "no root is indexed twice")

        val b = index.findClassFiles(ClassId.fromString("pb/B"), CLASS_FILES).single()
        assertEquals(classesB.localPath(), b.rootId.id)
        assertBackend(b)
        val c = index.findClassFiles(ClassId.fromString("pc/C"), CLASS_FILES).single()
        assertEquals(jarC.localPath(), c.rootId.id)
        assertBackend(c)

        val versionAfter = index.version
        environment.updateClasspath(listOf(classesB.toFile()))
        assertEquals(versionAfter, index.version, "re-adding indexed roots changes nothing")
        assertEquals(rootsAfter, index.indexedRoots.toList())
    }

    private fun configurationWithJdk(jdkHome: File): CompilerConfiguration =
        KotlinTestUtils.newConfiguration().apply {
            put(JVMConfigurationKeys.JDK_HOME, jdkHome)
        }

    private fun noJdkConfiguration(): CompilerConfiguration =
        KotlinTestUtils.newConfiguration().apply {
            put(JVMConfigurationKeys.NO_JDK, true)
        }

    private fun currentJdkHome(): File = File(System.getProperty("java.home"))

    private fun assumeModularTestJdk() {
        assumeTrue(File(currentJdkHome(), "lib/modules").isFile, "the test JVM must be a full modular JDK")
    }

    private fun findJdk8Home(): File? {
        val candidates = listOfNotNull(
            runCatching { KtTestUtil.getJdk8Home() }.getOrNull(),
            File("/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home"),
        )
        return candidates.firstOrNull { File(it, "jre/lib/rt.jar").isFile }
    }

    private fun JavaRoot.isJrtRoot(): Boolean = file.url.startsWith("jrt://")

    private fun JavaRoot.localPath(): String = file.path.removeSuffix("!/")

    private fun Path.localPath(): String = toFile().absolutePath.replace('\\', '/')

    private fun Path.createParentsAndWrite(): Path = apply {
        parent.createDirectories()
        writeBytes(ByteArray(0))
    }

    private fun Path.createParentsAndWriteText(text: String): Path = apply {
        parent.createDirectories()
        writeText(text)
    }

    private fun Path.createParentsAndWriteJar(vararg entries: String): Path = apply {
        parent.createDirectories()
        ZipOutputStream(outputStream()).use { out ->
            for (entry in entries) {
                out.putNextEntry(ZipEntry(entry))
                out.closeEntry()
            }
        }
    }

    protected companion object {
        val CLASS_FILES = JavaFileExtensions(JavaFileExtension.CLASS)
        val SIG_FILES = JavaFileExtensions(JavaFileExtension.SIG)
        val JAVA_FILES = JavaFileExtensions(JavaFileExtension.JAVA)
        val CLASS_AND_SIG_FILES = JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.SIG)
    }
}

open class PipelineJvmClasspathIndexRootsTest : JvmClasspathIndexRootsTest() {
    override fun createIndexedEnvironment(configuration: CompilerConfiguration): IndexedEnvironment {
        val pipelineConfiguration = configuration.copy().apply { testEnvironment = true }.withBackend()
        val projectEnvironment = JvmFrontendPipelinePhase.createProjectEnvironment(
            pipelineConfiguration, testRootDisposable
        )
        val project = projectEnvironment.project
        return IndexedEnvironment(
            (project.getService(VirtualFileFinderFactory::class.java) as CliVirtualFileFinderFactory).index,
            project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl,
            projectEnvironment::updateClasspath,
        )
    }
}

class NioJvmClasspathIndexRootsTest : JvmClasspathIndexRootsTest() {
    override val nioEntries: Boolean get() = true
}

class NioPipelineJvmClasspathIndexRootsTest : PipelineJvmClasspathIndexRootsTest() {
    override val nioEntries: Boolean get() = true
}
