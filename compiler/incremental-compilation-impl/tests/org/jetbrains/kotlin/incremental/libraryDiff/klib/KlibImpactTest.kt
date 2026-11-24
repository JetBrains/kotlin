/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.classpathDiff.ClassMembers
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KlibImpactTest {

    private val base = ClassId.fromString("com/example/Base")
    private val sub = ClassId.fromString("com/example/Sub")
    private val subSub = ClassId.fromString("com/example/SubSub")
    private val any = ClassId.fromString("kotlin/Any")

    private val snapshots = listOf(
        snapshot("Base.kt", KlibClassInfo(base, listOf(any))),
        snapshot("Sub.kt", KlibClassInfo(sub, listOf(base, any)), KlibClassInfo(subSub, listOf(sub))),
    )

    @Test
    fun testImpactedClasses() {
        val resolver = KlibAllImpacts.getResolver(snapshots)

        assertEquals(setOf(sub), resolver.getImpactedClasses(base))
        assertEquals(setOf(subSub), resolver.getImpactedClasses(sub))
        assertEquals(emptySet(), resolver.getImpactedClasses(subSub))
        assertEquals(emptySet(), resolver.getImpactedClasses(any))
    }

    @Test
    fun testImpactedClassMembers() {
        val resolver = KlibAllImpacts.getResolver(snapshots)

        assertEquals(
            setOf(ClassMembers(sub, setOf("member"))),
            resolver.getImpactedClassMembers(ClassMembers(base, setOf("member"))),
        )
    }

    @Test
    fun testImpactingClasses() {
        val reverseResolver = KlibAllImpacts.getReverseResolver(snapshots)

        assertEquals(setOf(sub), reverseResolver.getImpactingClasses(subSub))
        assertEquals(setOf(base), reverseResolver.getImpactingClasses(sub))
        assertEquals(emptySet(), reverseResolver.getImpactingClasses(base))
    }

    private fun snapshot(sourceFile: String, vararg classInfos: KlibClassInfo): KlibFileSnapshot =
        KlibFileSnapshot(
            contentHash = 0,
            packageName = FqName("com.example"),
            sourceFile = sourceFile,
            classInfos = classInfos.toList(),
            packageMemberNames = emptySet(),
            classMemberLevelSnapshot = null,
        )
}
