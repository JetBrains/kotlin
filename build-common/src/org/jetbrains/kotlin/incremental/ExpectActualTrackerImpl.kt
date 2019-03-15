/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import java.io.File

class ExpectActualTrackerImpl : ExpectActualTracker {
    private val expectToActual = hashMapOf<File, MutableSet<File>>()

    val expectToActualMap: Map<File, Set<File>>
        get() = expectToActual

    override fun report(expectedFile: File, actualFile: File) {
        expectToActual.getOrPut(expectedFile) { hashSetOf() }.add(actualFile)
    }
}