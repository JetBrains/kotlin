/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION") // the test is about the transitional state, see IncrementalCompilationComponentsWithCustomScope

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinder
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.IncrementalCompilationComponentsWithCustomScope
import org.jetbrains.kotlin.cli.jvm.compiler.prepareIncrementalCompilationContextAndLibrariesClasspath
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.config.VirtualJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalCompilationComponents
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.modules
import org.jetbrains.kotlin.config.outputDirectory
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.containsFile
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

/**
 * Emulates the IJ/Bazel build system's use of `IncrementalCompilationComponentsWithCustomScope` to pass the output of the
 * previous build as well as the recommended replacement via `VirtualJvmClasspathRoot.isPrecompiledOutput`.
 */
class IncrementalCompilationCustomScopeTest {

    private val disposable: Disposable = Disposer.newDisposable("${IncrementalCompilationCustomScopeTest::class.simpleName}.disposable")

    /**
     * Disposing a project disposes its `PsiManager`, which the platform allows in a write action only.
     * `CoreJarFileSystem` retains archive handles that prevent Windows `@TempDir` cleanup.
     */
    @AfterEach
    fun disposeEnvironment() {
        ApplicationManager.getApplication().runWriteAction { Disposer.dispose(disposable) }
        ZipHandler.clearFileAccessorCache()
    }

    @Test
    fun testTheScopeOfTheComponentsIsThePrecompiledBinariesClasspath(@TempDir tempDir: File) {
        val outputFileSystem = InMemoryOutputFileSystem()
        val components = ComponentsWithCustomScope(outputFileSystem.root)
        val [librariesClasspath, context] = prepareClasspaths(tempDir, components)

        val root = JvmClasspathRootId(IN_MEMORY_OUTPUT_ROOT_PATH.removeSuffix(JAR_SEPARATOR))
        assertTrue(components.wasAsked, "the components state the previous output as a scope")
        assertNotNull(context, "an incremental compilation has a precompiled-binaries session")
        assertEquals(JvmClasspath.Roots(listOf(root)), context?.precompiledBinaries)
        assertEquals(JvmClasspath.ProjectLibraries(excludedRoots = listOf(root)), librariesClasspath)
    }

    @Test
    fun testTheInMemoryRootRestrictsTheLookupToItself(@TempDir tempDir: File) {
        val outputFileSystem = InMemoryOutputFileSystem()
        val previousOutputClass = outputFileSystem.addFile("p/Ref.class")
        val localOutputClass = localClassFile(tempDir)
        val [_, context] = prepareClasspaths(tempDir, ComponentsWithCustomScope(outputFileSystem.root))

        val classpath = context!!.precompiledBinaries!!
        val scope = projectEnvironment.psiSearchScope(classpath)
        assertTrue(scope.contains(previousOutputClass), "the class file of the previous build lies under the root the components named")
        assertFalse(
            scope.contains(InMemoryOutputFileSystem().addFile("p/Ref.class")),
            "a file of another file system of the same shape is not under that root",
        )
        assertFalse(scope.contains(localOutputClass), "a file on disk is not under that root either")

        val otherFileSystemClass = InMemoryOutputFileSystem().addFile("p/Ref.class")
        assertTrue(classpath.containsFile(otherFileSystemClass), "root-ID matching intentionally ignores VirtualFile system identity")

        val finder = CliVirtualFileFinder(
            FixedJvmDependenciesIndex(listOf(previousOutputClass, localOutputClass)),
            classpath,
            enableSearchInCtSym = false,
            perfManager = null,
        )
        assertEquals(previousOutputClass, finder.findVirtualFileWithHeader(ClassId.fromString("p/Ref")))
        assertNull(finder.findVirtualFileWithHeader(ClassId.fromString("p/Local")))
    }

    @Test
    fun testTheRootIdFinderRestrictsLookupToTheLocalOutputDirectory(@TempDir tempDir: File) {
        val localOutputClass = localClassFile(tempDir)
        val [_, context] = prepareClasspaths(tempDir, PlainComponents())
        val finder = projectEnvironment.getKotlinClassFinder(context!!.precompiledBinaries!!) as VirtualFileFinder

        assertEquals(localOutputClass.path, finder.findVirtualFileWithHeader(ClassId.fromString("p/Local"))?.path)
    }

    /**
     * Pins the HMPP library-session root split (KT-88892): Kotlin is limited to common dependencies, while the Java facade uses the
     * complete libraries classpath. This records possibly-wrong current behavior, not a desired precedence rule.
     */
    @Test
    fun testHMPPCommonLibraryKotlinLookupAndJavaLookupUseDifferentRoots(@TempDir tempDir: File) {
        val [_, context] = prepareClasspaths(tempDir, PlainComponents())
        assertNotNull(context)

        val commonDependencyRoot = File(tempDir, "common-dependency").apply { mkdirs() }
        val platformLibraryRoot = File(tempDir, "platform-library").apply { mkdirs() }
        val platformOnlyClass = File(platformLibraryRoot, "p/JavaOnly.class").apply {
            parentFile.mkdirs()
            writeBytes(ByteArray(0))
        }

        val javaClassFile = localVirtualFile(platformOnlyClass)
        val index = FixedJvmDependenciesIndex(
            listOf(javaClassFile),
            sequenceOf(JavaRoot(localVirtualFile(platformLibraryRoot), JavaRoot.RootType.BINARY)),
        )
        val classId = ClassId.fromString("p/JavaOnly")
        val commonKotlinFinder = CliVirtualFileFinder(
            index,
            JvmClasspath.Roots(listOf(JvmClasspathRootId.of(commonDependencyRoot.toPath()))),
            enableSearchInCtSym = false,
            perfManager = null,
        )

        assertNull(commonKotlinFinder.findVirtualFileWithHeader(classId))
        assertTrue(JvmClasspath.ProjectLibraries().containsFile(javaClassFile))
    }

    @Test
    fun testPackagePartProviderUsesClasspathRoots(@TempDir tempDir: File) {
        prepareClasspaths(tempDir, PlainComponents())

        fun moduleMapping(packageFqName: String): ByteArray = JvmModuleProtoBuf.Module.newBuilder()
            .addPackageParts(
                JvmModuleProtoBuf.PackageParts.newBuilder()
                    .setPackageFqName(packageFqName)
                    .addShortClassName("PartKt")
            )
            .build()
            .serializeToByteArray(MetadataVersion.INSTANCE, flags = 0)

        val listedDirectory = tempDir.toPath().resolve("out").createDirectories()
        val nestedDirectory = listedDirectory.resolve("nested").createDirectories()
        listedDirectory.resolve("META-INF").createDirectories().resolve("test.kotlin_module").writeBytes(moduleMapping("listed"))
        nestedDirectory.resolve("META-INF").createDirectories().resolve("test.kotlin_module").writeBytes(moduleMapping("nested"))
        val jar = listedDirectory.resolve("lib.jar")
        ZipOutputStream(jar.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("META-INF/test.kotlin_module"))
            output.write(moduleMapping("jar"))
            output.closeEntry()
        }
        val jarPath = jar.toAbsolutePath().normalize().toString().replace('\\', '/')
        val jarRoot = checkNotNull(CoreJarFileSystem().findFileByPath(jarPath + JAR_SEPARATOR))
        val roots = listOf(
            JavaRoot(localVirtualFile(listedDirectory.toFile()), JavaRoot.RootType.BINARY),
            JavaRoot(localVirtualFile(nestedDirectory.toFile()), JavaRoot.RootType.BINARY),
            JavaRoot(jarRoot, JavaRoot.RootType.BINARY),
        )
        val classpathRoot = JvmClasspathRootId.of(listedDirectory)
        val configuration = CompilerConfiguration.create(messageCollector = MessageCollector.NONE)

        val rootProvider = JvmPackagePartProvider(configuration.languageVersionSettings, JvmClasspath.Roots(listOf(classpathRoot)))
        rootProvider.addRoots(roots, configuration)
        assertEquals(setOf("listed", "nested"), rootProvider.computePackageSetWithNonClassDeclarations())

        val libraryProvider = JvmPackagePartProvider(
            configuration.languageVersionSettings,
            JvmClasspath.ProjectLibraries(excludedRoots = listOf(classpathRoot)),
        )
        libraryProvider.addRoots(roots, configuration)
        assertEquals(setOf("jar"), libraryProvider.computePackageSetWithNonClassDeclarations())
    }

    @Test
    fun testAMarkedContentRootIsPreferredToTheScope(@TempDir tempDir: File) {
        val markedRoot = File(tempDir, "marked-output").apply { mkdirs() }
        val outputFileSystem = InMemoryOutputFileSystem()
        val components = ComponentsWithCustomScope(outputFileSystem.root)

        val [librariesClasspath, context] = prepareClasspaths(tempDir, components) {
            add(
                CLIConfigurationKeys.CONTENT_ROOTS,
                VirtualJvmClasspathRoot(localVirtualFile(markedRoot), isSdkRoot = false, isPrecompiledOutput = true),
            )
        }

        val expected = JvmClasspath.Roots(listOf(JvmClasspathRootId.of(markedRoot.toPath())))
        assertEquals(expected, context?.precompiledBinaries)
        assertEquals(JvmClasspath.ProjectLibraries(excludedRoots = expected.roots), librariesClasspath)
        assertFalse(components.wasAsked, "the marked roots make the transitional scope unnecessary")
    }

    @Test
    fun testWithoutTheScopeThePrecompiledBinariesAreTheOutputDirectory(@TempDir tempDir: File) {
        val [_, context] = prepareClasspaths(tempDir, PlainComponents())

        assertEquals(JvmClasspath.Roots(listOf(JvmClasspathRootId.of(outputDirectory(tempDir).toPath()))), context?.precompiledBinaries)
    }

    private lateinit var projectEnvironment: VfsBasedProjectEnvironment

    @OptIn(CoreEnvironmentDeprecation::class)
    private fun prepareClasspaths(
        tempDir: File,
        components: IncrementalCompilationComponents,
        configure: CompilerConfiguration.() -> Unit = {},
    ): Pair<JvmClasspath, IncrementalCompilationContext?> {
        val outputDirectory = outputDirectory(tempDir)
        val configuration = CompilerConfiguration.create(messageCollector = MessageCollector.NONE).apply {
            moduleName = MODULE_NAME
            this.outputDirectory = outputDirectory
            modules = listOf(ModuleBuilder(MODULE_NAME, outputDirectory.path, "java-production"))
            incrementalCompilationComponents = components
            addJvmClasspathRoot(outputDirectory)
            configure()
        }

        val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        projectEnvironment = environment.toVfsBasedProjectEnvironment()
        return prepareIncrementalCompilationContextAndLibrariesClasspath(configuration, projectEnvironment)
    }

    private fun outputDirectory(tempDir: File): File = File(tempDir, "out").apply { mkdirs() }

    private fun localClassFile(tempDir: File): VirtualFile {
        val file = File(tempDir, "out/p/Local.class").apply {
            parentFile.mkdirs()
            writeBytes(ByteArray(0))
        }
        return localVirtualFile(file)
    }

    private fun localVirtualFile(file: File): VirtualFile =
        checkNotNull(CoreLocalFileSystem().findFileByPath(file.invariantSeparatorsPath)) { "not in the VFS: $file" }

    /** The components of the IntelliJ build system, which name the previous output as a scope. */
    private class ComponentsWithCustomScope(private val outputRoot: VirtualFile) : IncrementalCompilationComponentsWithCustomScope {
        var wasAsked: Boolean = false
            private set

        override fun createSearchScope(projectEnvironment: VfsBasedProjectEnvironment): AbstractProjectFileSearchScope {
            wasAsked = true
            return PsiBasedProjectFileSearchScope(
                VfsBasedProjectEnvironment.DirectoriesScope(projectEnvironment.project, setOf(outputRoot))
            )
        }

        override fun getIncrementalCache(target: TargetId): IncrementalCache = EmptyIncrementalCache
    }

    private class PlainComponents : IncrementalCompilationComponents {
        override fun getIncrementalCache(target: TargetId): IncrementalCache = EmptyIncrementalCache
    }

    @Suppress("UNUSED_PARAMETER")
    private class FixedJvmDependenciesIndex(
        private val classFiles: List<VirtualFile>,
        override val indexedRoots: Sequence<JavaRoot> = emptySequence(),
    ) : JvmDependenciesIndex {

        override fun findClassVirtualFiles(classId: ClassId, acceptedExtensions: JavaFileExtensions): Collection<VirtualFile> =
            classFiles.filter { it.nameWithoutExtension == classId.relativeClassName.asString() }

        override fun traverseClassVirtualFilesInPackage(
            packageFqName: FqName,
            acceptedExtensions: JavaFileExtensions,
            continueSearch: (VirtualFile) -> Boolean,
        ) {
        }

        override fun traverseDirectoriesInPackage(
            packageFqName: FqName,
            acceptedRootTypes: Set<JavaRoot.RootType>,
            continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean,
        ) {
        }

        override fun traverseVirtualFilesInPackage(
            packageFqName: FqName,
            acceptedRootTypes: Set<JavaRoot.RootType>,
            continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean,
        ) {
        }
    }

    private object EmptyIncrementalCache : IncrementalCache {
        override fun getObsoletePackageParts(): Collection<String> = emptyList()
        override fun getObsoleteMultifileClasses(): Collection<String> = emptyList()
        override fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>? = null
        override fun getPackagePartData(partInternalName: String): JvmPackagePartProto? = null
        override fun getModuleMappingData(): ByteArray? = null
        override fun getMetadata(fragmentName: String): Map<File, ByteArray> = emptyMap()
        override fun getClassFilePath(internalClassName: String): String = internalClassName
        override fun close() {}
    }

    /** See the class documentation; the IntelliJ original is `OutputFileSystem`. */
    private class InMemoryOutputFileSystem : DeprecatedVirtualFileSystem() {
        val root: InMemoryOutputFile = InMemoryOutputFile(this, name = "", parent = null, isDirectory = true)

        fun addFile(relativePath: String): VirtualFile {
            val names = relativePath.split('/')
            return names.foldIndexed(root) { index, parent, name ->
                parent.child(name, isDirectory = index < names.lastIndex)
            }
        }

        override fun getProtocol(): String = StandardFileSystems.JAR_PROTOCOL

        override fun findFileByPath(path: String): VirtualFile? =
            root.takeIf { path.removeSuffix(JAR_SEPARATOR) == IN_MEMORY_OUTPUT_ROOT_PATH.removeSuffix(JAR_SEPARATOR) }
                ?: root.findFileByRelativePath(path.substringAfter(JAR_SEPARATOR))

        override fun refresh(asynchronous: Boolean) {}

        override fun refreshAndFindFileByPath(path: String): VirtualFile? = findFileByPath(path)
    }

    private class InMemoryOutputFile(
        private val fileSystem: InMemoryOutputFileSystem,
        private val name: String,
        private val parent: InMemoryOutputFile?,
        private val isDirectory: Boolean,
    ) : VirtualFile() {
        private val children = linkedMapOf<String, InMemoryOutputFile>()

        fun child(name: String, isDirectory: Boolean): InMemoryOutputFile =
            children.getOrPut(name) { InMemoryOutputFile(fileSystem, name, this, isDirectory) }

        override fun getName(): String = name
        override fun getFileSystem(): VirtualFileSystem = fileSystem
        override fun getPath(): String = parent?.let { "${it.path.removeSuffix("/")}/$name" } ?: IN_MEMORY_OUTPUT_ROOT_PATH
        override fun isWritable(): Boolean = false
        override fun isDirectory(): Boolean = isDirectory
        override fun isValid(): Boolean = true
        override fun getParent(): VirtualFile? = parent
        override fun getChildren(): Array<VirtualFile> = children.values.toTypedArray()
        override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
            throw UnsupportedOperationException()

        override fun contentsToByteArray(): ByteArray = ByteArray(0)
        override fun getTimeStamp(): Long = 0
        override fun getLength(): Long = 0
        override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
        override fun getInputStream(): InputStream = ByteArrayInputStream(contentsToByteArray())
        override fun getModificationStamp(): Long = 0
    }

    private companion object {
        const val MODULE_NAME = "module-in-memory"

        /** The path of the root of the IntelliJ output file system, as [InMemoryOutputFileSystem] names it too. */
        const val IN_MEMORY_OUTPUT_ROOT_PATH = "__module_in-memory__output__!/"
    }
}
