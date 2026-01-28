/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import java.io.File

class ExpectActualTrackerImpl : ExpectActualTracker {
    val expectToActualMap: Map<File, Set<File>>
        field = hashMapOf<File, MutableSet<File>>()

    val expectsOfLenientStubsSet: Set<File>
        field = hashSetOf<File>()

    override fun report(expectedFile: File, actualFile: File) {
        expectToActualMap.getOrPut(expectedFile) { hashSetOf() }.add(actualFile)
    }

    override fun reportExpectOfLenientStub(expectedFile: File) {
        expectsOfLenientStubsSet.add(expectedFile)
    }
}