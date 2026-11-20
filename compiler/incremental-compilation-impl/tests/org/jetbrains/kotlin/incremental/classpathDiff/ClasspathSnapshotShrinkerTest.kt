/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.CompileUtil.compileAll
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

private val testDataDir =
    ForTestCompileRuntime.transformTestDataPath("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathChangesComputerTest")

private val snapshotterTestDataDir =
    ForTestCompileRuntime.transformTestDataPath("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathSnapshotterTest")

class ClasspathSnapshotShrinkerTest : ClasspathSnapshotTestCommon() {

    private fun shrinkTestClasspath(vararg lookupSymbols: Pair<String, String>): Set<String> =
        shrinkTestClasspath("JavaOnly/testPackageInfoChange/src/current-classpath", *lookupSymbols)

    private fun shrinkTestClasspath(classpathSourceDir: String, vararg lookupSymbols: Pair<String, String>): Set<String> {
        val allClasses = snapshotClasspath(File(testDataDir, classpathSourceDir), tmpDir).removeDuplicateAndInaccessibleClasses()
        val lookupSymbolKeys = lookupSymbols.map { [scope, name] -> LookupSymbolKey(name.hashCode(), scope.hashCode(), name, scope) }
        return ClasspathSnapshotShrinker.shrinkClasses(allClasses, lookupSymbolKeys).mapTo(mutableSetOf()) { it.classId.asString() }
    }

    @Test
    fun testPackageInfoIsRetainedWhenClassInPackageIsReferenced() {
        // Only a member of the class is looked up, so there is no lookup with the package as scope. The package-info must still be retained,
        // otherwise a later change in it could not be detected.
        val retainedClasses = shrinkTestClasspath("com.example.ClassInPackage" to "someMethod")

        assertEquals(setOf("com/example/ClassInPackage", "com/example/package-info"), retainedClasses)
    }

    @Test
    fun testPackageInfoIsRetainedWhenSubclassInOtherPackageIsReferenced() {
        val retainedClasses = shrinkTestClasspath("com.other" to "SubClassOfClassInPackage")

        assertEquals(
            setOf("com/other/SubClassOfClassInPackage", "com/example/ClassInPackage", "com/example/package-info"),
            retainedClasses
        )
    }

    @Test
    fun testPackageInfoIsNotRetainedWhenPackageIsNotReferenced() {
        val retainedClasses = shrinkTestClasspath("com.other" to "UnimpactedClass")

        assertEquals(setOf("com/other/UnimpactedClass"), retainedClasses)
    }

    /**
     * Test classpath (`JavaOnly/testModuleInfoChange/src/previous-classpath`): `app` reads `apkg` only through `mid`, which
     * `requires transitive base`; `other` is unrelated.
     *   - `base` : apkg.A
     *   - `mid`  : mpkg.M, requires transitive base
     *   - `other`: opkg.O
     */
    private fun shrinkModularTestClasspath(vararg lookupSymbols: Pair<String, String>): Set<String> =
        shrinkTestClasspath("JavaOnly/testModuleInfoChange/src/previous-classpath", *lookupSymbols)

    @Test
    fun testModuleInfoIsRetainedWhenClassOfModuleIsReferenced() {
        // `base.module-info` governs `apkg.A`; `mid.module-info` may grant (or drop) readability of `base` through `requires transitive`
        assertEquals(setOf("apkg/A", "base/module-info", "mid/module-info"), shrinkModularTestClasspath("apkg.A" to "someMethod"))
    }

    @Test
    fun testModuleInfoIsNotRetainedWhenModuleIsNotReferenced() {
        assertEquals(setOf("opkg/O", "other/module-info"), shrinkModularTestClasspath("opkg" to "O"))
    }

    @Test
    fun testModuleInfosOfDifferentEntriesAreNotDeduplicated() {
        // Each entry is compiled on its own as `first` and `sameAsFirst` declare the same module
        val [first, second, sameAsFirst] = listOf("first", "second", "sameAsFirst").map {
            compileAll(File(snapshotterTestDataDir, "java/testModuleInfoDuplicates/src/$it"), tmpDir).snapshotClasspathEntry()
        }

        val classes = ClasspathSnapshot(listOf(first, second, sameAsFirst)).removeDuplicateAndInaccessibleClasses()

        // Descriptors of different modules are retained even though they share the `module-info.class` path; `apkg/A` and the second
        // `first.mod` descriptor are duplicates, so the first occurrence wins
        assertEquals(listOf("apkg/A", "first/mod/module-info", "second/mod/module-info"), classes.map { it.classId.asString() })
        assertEquals(
            (first.classSnapshots.getValue("module-info.class") as ModuleInfoClassSnapshot).classAbiHash,
            classes.filterIsInstance<ModuleInfoClassSnapshot>().first().classAbiHash
        )
    }
}
