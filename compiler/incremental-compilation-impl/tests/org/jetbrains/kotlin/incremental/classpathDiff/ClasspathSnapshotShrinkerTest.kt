/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

private val testDataDir =
    ForTestCompileRuntime.transformTestDataPath("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathChangesComputerTest")

class ClasspathSnapshotShrinkerTest : ClasspathSnapshotTestCommon() {

    private fun shrinkTestClasspath(vararg lookupSymbols: Pair<String, String>): Set<String> {
        val allClasses = snapshotClasspath(File(testDataDir, "JavaOnly/testPackageInfoChange/src/current-classpath"), tmpDir)
            .removeDuplicateAndInaccessibleClasses()
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
}
