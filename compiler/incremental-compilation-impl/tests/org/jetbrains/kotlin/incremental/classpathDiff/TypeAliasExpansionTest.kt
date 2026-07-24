/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.jetbrains.kotlin.incremental.storage.fromByteArray
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeAliasExpansionTest {

    private fun classId(name: String) = ClassId.topLevel(FqName("com.example.$name"))

    private val aliasClassId = classId("A")
    private val expandedClassId = classId("B")
    private val facadeClassId = classId("AliasesKt")

    @Test
    fun `change in expanded class also surfaces as a change in the alias`() {
        val changes = ClasspathChangesComputer.computeChangedAndImpactedSet(
            currentClassSnapshots = listOf(regularClass(expandedClassId, classAbiHash = 2L), facade()),
            previousClassSnapshots = listOf(regularClass(expandedClassId, classAbiHash = 1L), facade()),
            reporter = ClasspathSnapshotBuildReporter(DoNothingBuildReporter)
        )

        val changesEither = changes.toChangesEither()

        // The change in `B` must also surface as a change in the alias `A`, so that consumers referencing `A` are recompiled.
        assertEquals(
            setOf(
                LookupSymbol(name = "B", scope = "com.example"),
                LookupSymbol(name = "A", scope = "com.example"),
            ),
            changesEither.lookupSymbols.toSet()
        )
        assertEquals(
            setOf(FqName("com.example.B"), FqName("com.example.A")),
            changesEither.fqNames.toSet()
        )
    }

    @Test
    fun `expanded class is retained when only the alias is referenced`() {
        val unrelatedClassId = classId("Unrelated")
        val allClasses = listOf(
            facade(),
            regularClass(expandedClassId, classAbiHash = 1L),
            regularClass(unrelatedClassId, classAbiHash = 1L),
        )
        val lookupSymbols = listOf(LookupSymbolKey(name = "A", scope = "com.example"))

        val retainedClassIds = ClasspathSnapshotShrinker.shrinkClasses(allClasses, lookupSymbols).map { it.classId }.toSet()

        // The facade (declaring the alias) is directly referenced; the expanded class `B` must be retained transitively so that later
        // changes in `B` can still be detected as changes in `A`. The unrelated class must be dropped.
        assertTrue(facadeClassId in retainedClassIds) { "Facade should be retained: $retainedClassIds" }
        assertTrue(expandedClassId in retainedClassIds) { "Expanded class should be retained: $retainedClassIds" }
        assertTrue(unrelatedClassId !in retainedClassIds) { "Unrelated class should be dropped: $retainedClassIds" }
    }

    @Test
    fun `type aliases survive snapshot serialization round-trip`() {
        val facade = PackageFacadeKotlinClassSnapshot(
            classId = facadeClassId,
            classAbiHash = 42L,
            classMemberLevelSnapshot = null,
            packageMemberNames = setOf("A", "F"),
            typeAliases = listOf(
                TypeAliasSnapshot(aliasClassId, expandedClassId),
                TypeAliasSnapshot(classId("F"), ClassId.topLevel(FqName("kotlin.Function0"))),
            )
        )
        val snapshot = ClasspathEntrySnapshot(linkedMapOf("com/example/AliasesKt.class" to (facade as ClassSnapshot)))

        val restored = ClasspathEntrySnapshotExternalizer.fromByteArray(ClasspathEntrySnapshotExternalizer.toByteArray(snapshot))

        val restoredFacade = restored.classSnapshots.values.single() as PackageFacadeKotlinClassSnapshot
        assertEquals(facade.typeAliases, restoredFacade.typeAliases)
    }

    private fun facade(typeAliases: List<TypeAliasSnapshot> = listOf(TypeAliasSnapshot(aliasClassId, expandedClassId))) =
        PackageFacadeKotlinClassSnapshot(
            classId = facadeClassId,
            classAbiHash = 100L,
            classMemberLevelSnapshot = null,
            packageMemberNames = setOf("A"),
            typeAliases = typeAliases
        )

    private fun regularClass(classId: ClassId, classAbiHash: Long) = RegularKotlinClassSnapshot(
        classId = classId,
        classAbiHash = classAbiHash,
        classMemberLevelSnapshot = null,
        supertypes = emptyList(),
        companionObjectName = null,
        constantsInCompanionObject = null
    )
}
