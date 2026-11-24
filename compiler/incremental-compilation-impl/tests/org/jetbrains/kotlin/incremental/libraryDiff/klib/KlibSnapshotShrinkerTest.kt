/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

/** Unlike `ClasspathSnapshotShrinkerTest`, there are no `package-info` / `module-info` cases: klibs don't have them. */
class KlibSnapshotShrinkerTest : KlibSnapshotTestCommon() {

    private val allSnapshots: List<KlibFileSnapshot> by lazy {
        snapshotKlib(File(klibTestDataDir, "KlibSnapshotShrinkerTest/src")).klibFileSnapshots
    }

    private fun shrink(vararg lookupSymbols: LookupSymbolKey): Set<String> =
        KlibSnapshotShrinker.shrink(allSnapshots, lookupSymbols.toList()).mapTo(mutableSetOf()) { it.sourceFilePath }

    @Test
    fun testReferencedClassIsRetained() {
        assertEquals(setOf("com/example/Base.kt"), shrink(LookupSymbolKey(name = "Base", scope = "com.example")))
    }

    @Test
    fun testSupertypesOfReferencedClassAreRetained() {
        assertEquals(setOf("com/example/Sub.kt", "com/example/Base.kt"), shrink(LookupSymbolKey(name = "Sub", scope = "com.example")))
    }

    @Test
    fun testClassReferencedByMemberLookupIsRetained() {
        assertEquals(setOf("com/example/Base.kt"), shrink(LookupSymbolKey(name = "someMember", scope = "com.example.Base")))
    }

    @Test
    fun testReferencedPackageMemberIsRetained() {
        assertEquals(setOf("com/example/TopLevel.kt"), shrink(LookupSymbolKey(name = "topLevelFunction", scope = "com.example")))
    }

    @Test
    fun testSupertypesOfUnreferencedClassesInRetainedFileAreNotRetained() {
        assertEquals(
            setOf("com/example/SubAndTopLevel.kt"),
            shrink(LookupSymbolKey(name = "otherTopLevelFunction", scope = "com.example")),
        )
    }

    @Test
    fun testNothingIsRetainedWithoutLookups() {
        assertEquals(emptySet(), shrink())
    }
}
