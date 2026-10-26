/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.ClassFileUtil.snapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.CompileUtil.compileAll
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.SourceFile.JavaSourceFile
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.SourceFile.KotlinSourceFile
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.TestSourceFile
import org.jetbrains.kotlin.incremental.classpathDiff.ModuleInfoClassSnapshot.Companion.moduleInfoClassId
import org.jetbrains.kotlin.incremental.classpathDiff.impl.ClassFile
import org.jetbrains.kotlin.incremental.storage.fromByteArray
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

private val testDataDir =
    ForTestCompileRuntime.transformTestDataPath("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathSnapshotterTest")

class KotlinOnlyClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    @Suppress("SameParameterValue")
    private fun getSourceFile(testName: String, relativePath: String) = TestSourceFile(
        KotlinSourceFile(
            baseDir = File("$testDataDir/kotlin/$testName/src"), relativePath = relativePath,
            preCompiledClassFile = ClassFile(File("$testDataDir/kotlin/$testName/classes"), relativePath.replace(".kt", ".class"))
        ), tmpDir
    )

    @Test
    fun testSimpleClass() {
        val sourceFile = getSourceFile("testSimpleClass", "com/example/SimpleClass.kt")
        val actualSnapshot = sourceFile.compileAndSnapshot().toGson()
        val expectedSnapshot = sourceFile.getExpectedSnapshotText()

        assertEquals(expectedSnapshot, actualSnapshot)

        // Check that the snapshot contains ABI info
        actualSnapshot.assertContains("publicProperty", "publicFunction")

        // Private properties and functions' names/signatures are currently part of the snapshot. We will fix this later.
        actualSnapshot.assertContains("privateProperty", "privateFunction")

        // Check that the snapshot does not contain non-ABI info
        actualSnapshot.assertDoesNotContain(
            "publicProperty's value",
            "privateProperty's value",
            "publicFunction's body",
            "privateFunction's body"
        )
    }

    @Test
    fun testSimpleClass_ClassLevelSnapshot() {
        val sourceFile = getSourceFile("testSimpleClass", "com/example/SimpleClass.kt")
        val classFile = sourceFile.compileSingle()
        val actualSnapshot = classFile.snapshot(ClassSnapshotGranularity.CLASS_LEVEL).toGson()
        val expectedSnapshot = sourceFile.getExpectedSnapshotText(ClassSnapshotGranularity.CLASS_LEVEL)

        assertEquals(expectedSnapshot, actualSnapshot)

        // Check that the snapshot does not contain class member details
        actualSnapshot.assertDoesNotContain("publicProperty", "privateProperty", "publicFunction", "privateFunction")
    }

    @Test
    fun testPackageFacadeClasses() {
        val classpathSnapshot = snapshotClasspath(File("$testDataDir/kotlin/testPackageFacadeClasses/src"), tmpDir)
        val classSnapshots = classpathSnapshot.classpathEntrySnapshots.single().classSnapshots
        val fileFacadeSnapshot = classSnapshots["com/example/FileFacadeKt.class"]!!.toGson()
        val multifileClassSnapshot = classSnapshots["com/example/MultifileClass.class"]!!.toGson()
        val multifileClassPart1Snapshot = classSnapshots["com/example/MultifileClass__MultifileClass1Kt.class"]!!.toGson()
        val multifileClassPart2Snapshot = classSnapshots["com/example/MultifileClass__MultifileClass2Kt.class"]!!.toGson()

        // Check that the snapshots contain ABI info
        fileFacadeSnapshot.assertContains("propertyInFileFacade", "functionInFileFacade")
        multifileClassPart1Snapshot.assertContains("propertyInMultifileClass1", "functionInMultifileClass1")
        multifileClassPart2Snapshot.assertContains("propertyInMultifileClass2", "functionInMultifileClass2")

        // Check that the snapshots do not contain non-ABI info
        fileFacadeSnapshot.assertDoesNotContain("propertyInFileFacade's value", "functionInFileFacade's body")
        multifileClassPart1Snapshot.assertDoesNotContain("propertyInMultifileClass1's value", "functionInMultifileClass1's body")
        multifileClassPart2Snapshot.assertDoesNotContain("propertyInMultifileClass2's value", "functionInMultifileClass2's body")

        // Classes with MULTIFILE_CLASS kind have no proto data
        multifileClassSnapshot.assertDoesNotContain(
            "propertyInMultifileClass1",
            "functionInMultifileClass1",
            "propertyInMultifileClass2",
            "functionInMultifileClass2"
        )
    }
}

/** Tests snapshotting of top-level type aliases, enabled by [ClasspathEntrySnapshotter.Settings.expandTypeAliases]. */
class TypeAliasExpansionClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    private val aliasesSourceFile by lazy {
        TestSourceFile(
            KotlinSourceFile(
                baseDir = File("$testDataDir/kotlin/testTypeAliasExpansion/src"), relativePath = "com/example/aliases.kt",
                preCompiledClassFile = ClassFile(
                    File("$testDataDir/kotlin/testTypeAliasExpansion/classes"), "com/example/AliasesKt.class"
                )
            ), tmpDir
        )
    }

    private fun classId(packageName: String, relativeClassName: String) =
        ClassId(FqName(packageName), FqName(relativeClassName), false)

    /** `PrivateAlias` is deliberately absent from the expected value: private type aliases are not part of the ABI. */
    private val expectedTypeAliases = listOf(
        TypeAliasSnapshot(classId("com.example", "PublicAlias"), classId("com.example", "B")),
        TypeAliasSnapshot(classId("com.example", "NestedAlias"), classId("com.example", "B.Nested")),
    )

    @Test
    fun testTypeAliasExpansionsAreRecordedWhenEnabled() {
        val snapshot = aliasesSourceFile.compileSingle().snapshot(expandTypeAliases = true)

        assertEquals(expectedTypeAliases, (snapshot as PackageFacadeKotlinClassSnapshot).typeAliases)
    }

    @Test
    fun testTypeAliasExpansionsSurviveSerialization() {
        val snapshot = aliasesSourceFile.compileSingle().snapshot(expandTypeAliases = true)
        val deserialized = ClassSnapshotExternalizer.fromByteArray(ClassSnapshotExternalizer.toByteArray(snapshot))

        assertEquals(expectedTypeAliases, (deserialized as PackageFacadeKotlinClassSnapshot).typeAliases)
    }

    @Test
    fun testTypeAliasExpansionsAreNotRecordedWhenDisabled() {
        val snapshot = aliasesSourceFile.compileSingle().snapshot()

        assertNull((snapshot as PackageFacadeKotlinClassSnapshot).typeAliases)
    }

    /** KT-90252: enabling the setting must be visible as an ABI change of the facade, otherwise its new aliases are never diffed. */
    @Test
    fun testTypeAliasExpansionsAffectClassAbiHash() {
        val classFile = aliasesSourceFile.compileSingle()

        assertNotEquals(
            (classFile.snapshot() as AccessibleClassSnapshot).classAbiHash,
            (classFile.snapshot(expandTypeAliases = true) as AccessibleClassSnapshot).classAbiHash
        )
    }

    /** Facades without type aliases must keep their hash when the setting is toggled, to avoid needless rebuilds. */
    @Test
    fun testTypeAliasExpansionsDoNotAffectClassAbiHashWithoutAliases() {
        val classFile = TestSourceFile(
            KotlinSourceFile(
                baseDir = File("$testDataDir/kotlin/testPackageFacadeClasses/src"), relativePath = "com/example/FileFacade.kt",
                preCompiledClassFile = ClassFile(
                    File("$testDataDir/kotlin/testPackageFacadeClasses/classes"), "com/example/FileFacadeKt.class"
                )
            ), tmpDir
        ).compileSingle()

        assertEquals(
            (classFile.snapshot() as AccessibleClassSnapshot).classAbiHash,
            (classFile.snapshot(expandTypeAliases = true) as AccessibleClassSnapshot).classAbiHash
        )
    }
}

class JavaOnlyClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    @Suppress("SameParameterValue")
    private fun getSourceFile(testName: String, relativePath: String) = TestSourceFile(
        JavaSourceFile(baseDir = File("$testDataDir/java/$testName/src"), relativePath = relativePath), tmpDir
    )

    @Test
    fun testSimpleClass() {
        val sourceFile = getSourceFile("testSimpleClass", "com/example/SimpleClass.java")
        val actualSnapshot = sourceFile.compileAndSnapshot().toGson()
        val expectedSnapshot = sourceFile.getExpectedSnapshotText()

        assertEquals(expectedSnapshot, actualSnapshot)

        // Check that the snapshot contains ABI info
        actualSnapshot.assertContains("publicField", "publicMethod")

        // Check that the snapshot does not contain non-ABI info
        actualSnapshot.assertDoesNotContain(
            "privateField",
            "privateMethod",
            "publicField's value",
            "privateField's value",
            "publicMethod's body",
            "privateMethod's body"
        )
    }

    @Test
    fun testSimpleClass_ClassLevelSnapshot() {
        val sourceFile = getSourceFile("testSimpleClass", "com/example/SimpleClass.java")
        val classFile = sourceFile.compileSingle()
        val actualSnapshot = classFile.snapshot(ClassSnapshotGranularity.CLASS_LEVEL).toGson()
        val expectedSnapshot = sourceFile.getExpectedSnapshotText(ClassSnapshotGranularity.CLASS_LEVEL)

        assertEquals(expectedSnapshot, actualSnapshot)

        // Check that the snapshot does not contain class member details
        actualSnapshot.assertDoesNotContain("publicField", "privateField", "publicMethod", "privateMethod")
    }

    @Test
    fun testPackageInfo() {
        val classFile = getSourceFile("testPackageInfo", "com/example/package-info.java").compileSingle()
        val classLevelSnapshot = classFile.snapshot(ClassSnapshotGranularity.CLASS_LEVEL)
        val memberLevelSnapshot = classFile.snapshot(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL)

        // `package-info.class` is synthetic, but it must not be treated as inaccessible: its annotations are part of the package's ABI
        assertInstanceOf(PackageInfoClassSnapshot::class.java, classLevelSnapshot)
        assertEquals(ClassId(FqName("com.example"), FqName("package-info"), false), (classLevelSnapshot as PackageInfoClassSnapshot).classId)

        // Granularity is forced to CLASS_LEVEL
        assertInstanceOf(PackageInfoClassSnapshot::class.java, memberLevelSnapshot)
        assertEquals(classLevelSnapshot.classAbiHash, (memberLevelSnapshot as PackageInfoClassSnapshot).classAbiHash)
    }
}

/** Tests snapshotting of `module-info.class` (see [ModuleInfoClassSnapshot]) and the selection of the descriptor within a classpath entry. */
class ModuleInfoClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    private val settings = ClasspathEntrySnapshotter.Settings(
        granularity = ClassSnapshotGranularity.CLASS_LEVEL,
        parseInlinedLocalClasses = false,
        expandTypeAliases = false,
    )

    /** Compiles the module in `java/<testName>/src/<moduleDir>` on its own and returns its classes directory. */
    private fun compileModule(testName: String, moduleDir: String, vararg javacOptions: String): File =
        compileAll(File("$testDataDir/java/$testName/src/$moduleDir"), tmpDir, javacOptions = javacOptions.toList()).first().classRoot

    private fun File.moduleInfoSnapshot(): ModuleInfoClassSnapshot? =
        ClasspathEntrySnapshotter.snapshot(this, settings).classSnapshots.values.filterIsInstance<ModuleInfoClassSnapshot>().singleOrNull()

    @Test
    fun testModuleInfo() {
        val classpathSnapshot = snapshotClasspath(File("$testDataDir/java/testModuleInfo/src"), tmpDir)
        val classSnapshots = classpathSnapshot.classpathEntrySnapshots.single().classSnapshots
        val actualSnapshot = classSnapshots["module-info.class"] as ModuleInfoClassSnapshot
        val expectedSnapshot = File("$testDataDir/java/testModuleInfo/expected-snapshot/module-info.json").readText().trimEnd()

        assertEquals(expectedSnapshot, actualSnapshot.toGson())
        assertEquals(moduleInfoClassId("some.mod"), actualSnapshot.classId)
        assertEquals("some.mod", actualSnapshot.moduleName)
        assertEquals(setOf(FqName("apkg")), actualSnapshot.packages)
        assertEquals(listOf("java.sql"), actualSnapshot.transitiveRequires)

        // The same descriptor must be produced when the classpath entry is snapshotted as a whole
        assertEquals(expectedSnapshot, compileModule("testModuleInfo", "some.mod").moduleInfoSnapshot()!!.toGson())
    }

    @Test
    fun testModuleVersionsDoNotAffectSnapshot() {
        val v1 = compileModule("testModuleInfo", "some.mod", "--module-version", "1.0").moduleInfoSnapshot()!!
        val v2 = compileModule("testModuleInfo", "some.mod", "--module-version", "2.0").moduleInfoSnapshot()!!

        assertEquals(v1.classAbiHash, v2.classAbiHash)
    }

    @Test
    fun testDirectiveOrderDoesNotAffectSnapshot() {
        val base = compileModule("testModuleInfoDescriptorVariants", "base").moduleInfoSnapshot()!!
        val reordered = compileModule("testModuleInfoDescriptorVariants", "reorderedDirectives").moduleInfoSnapshot()!!

        assertEquals(base.classAbiHash, reordered.classAbiHash)
    }

    @Test
    fun testDescriptorChangesAffectSnapshot() {
        val variants = listOf("base", "removedExports", "addedRequires", "transitiveRequires", "qualifiedExports", "annotated", "open")
        val hashes = variants.map { compileModule("testModuleInfoDescriptorVariants", it).moduleInfoSnapshot()!!.classAbiHash }

        assertEquals(hashes.size, hashes.toSet().size, "All descriptors must have distinct hashes: ${variants.zip(hashes)}")
    }

    @Test
    fun testVersionedModuleInfoIsIgnoredInDirectory() {
        val classesDir = compileModule("testModuleInfoSelection", "root")
        val versionedDir = classesDir.resolve("META-INF/versions/9").apply { mkdirs() }
        assertTrue(classesDir.resolve("module-info.class").renameTo(versionedDir.resolve("module-info.class")))

        assertNull(classesDir.moduleInfoSnapshot())
    }

    /** Builds a jar at [jarName] with the given manifest attributes and entries (jar path to source file). */
    private fun buildJar(jarName: String, multiRelease: Boolean?, entries: Map<String, File>): File {
        val jar = tmpDir.resolve(jarName)
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            multiRelease?.let { mainAttributes.putValue("Multi-Release", it.toString()) }
        }
        JarOutputStream(jar.outputStream(), manifest).use { out ->
            entries.forEach { [path, file] ->
                out.putNextEntry(JarEntry(path))
                out.write(file.readBytes())
                out.closeEntry()
            }
        }
        return jar
    }

    @Test
    fun testRootModuleInfoWinsOverVersionedOnesInJar() {
        val root = compileModule("testModuleInfoSelection", "root")
        val versioned = compileModule("testModuleInfoSelection", "v9")
        val jar = buildJar(
            "root.jar", multiRelease = true, entries = mapOf(
                "module-info.class" to root.resolve("module-info.class"),
                "META-INF/versions/9/module-info.class" to versioned.resolve("module-info.class"),
                "apkg/A.class" to root.resolve("apkg/A.class"),
            )
        )

        assertEquals("root.mod", jar.moduleInfoSnapshot()!!.moduleName)
    }

    @Test
    fun testLowestVersionedModuleInfoIsSelectedInMultiReleaseJar() {
        val v8 = compileModule("testModuleInfoSelection", "v8")
        val v9 = compileModule("testModuleInfoSelection", "v9")
        val v11 = compileModule("testModuleInfoSelection", "v11")
        val entries = mapOf(
            "META-INF/versions/11/module-info.class" to v11.resolve("module-info.class"),
            "META-INF/versions/9/module-info.class" to v9.resolve("module-info.class"),
            "META-INF/versions/8/module-info.class" to v8.resolve("module-info.class"),
            "apkg/A.class" to v9.resolve("apkg/A.class"),
        )

        assertEquals("v9.mod", buildJar("multi-release.jar", multiRelease = true, entries).moduleInfoSnapshot()!!.moduleName)
        // Versioned descriptors are only considered for multi-release jars
        assertNull(buildJar("not-multi-release.jar", multiRelease = false, entries).moduleInfoSnapshot())
        assertNull(buildJar("no-attribute.jar", multiRelease = null, entries).moduleInfoSnapshot())
    }

    @Test
    fun testVersionedModuleInfoBelowJava9IsIgnored() {
        val v8 = compileModule("testModuleInfoSelection", "v8")
        val jar = buildJar(
            "v8.jar", multiRelease = true, entries = mapOf(
                "META-INF/versions/8/module-info.class" to v8.resolve("module-info.class"),
                "apkg/A.class" to v8.resolve("apkg/A.class"),
            )
        )

        assertNull(jar.moduleInfoSnapshot())
    }
}

private fun TestSourceFile.getExpectedSnapshotText(granularity: ClassSnapshotGranularity? = null): String {
    val relativePath = sourceFile.unixStyleRelativePath.substringBeforeLast(".") + ".json"
    val expectedSnapshotDirName = if (granularity == null) "expected-snapshot" else "expected-snapshot-${granularity.name}"
    return sourceFile.baseDir.resolve("../$expectedSnapshotDirName/$relativePath").readText().trimEnd()
}

private fun String.assertContains(vararg elements: String) {
    elements.forEach {
        assertTrue(contains(it))
    }
}

private fun String.assertDoesNotContain(vararg elements: String) {
    elements.forEach {
        assertFalse(contains(it))
    }
}
