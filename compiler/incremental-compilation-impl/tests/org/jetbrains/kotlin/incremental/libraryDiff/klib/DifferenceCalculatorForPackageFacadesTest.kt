/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacades
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class DifferenceCalculatorForPackageFacadesTest : KlibSnapshotTestCommon() {

    private val testDataDir = File(klibTestDataDir, "DifferenceCalculatorForPackageFacadesTest")

    /**
     * A name is reported if any of its declarations changed:
     *   - `duplicated` is declared in two klibs, and only the second klib's copy changes.
     *   - `overloaded` has different overloads in two files, and only one of them changes.
     */
    @Test
    fun testDuplicateMembers() {
        val difference = DifferenceCalculatorForPackageFacades(
            oldData = packageParts("previous") + packageParts("otherKlibPrevious"),
            newData = packageParts("current") + packageParts("otherKlibCurrent"),
        ).difference()

        assertEquals(setOf("duplicated", "overloaded"), difference.changedMembersNames)
    }

    private fun packageParts(version: String): List<PackagePartProtoData> =
        snapshotKlib(File(testDataDir, "testDuplicateMembers/src/$version")).klibFileSnapshots
            .map { it.classMemberLevelSnapshot!!.packagePartProtoData }
}
