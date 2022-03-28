/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.ReflektTracker
import java.io.File

class ReflektTrackerImpl : ReflektTracker {
    private val fileToReflektUsage = hashMapOf<File, MutableSet<File>>()

    val fileToReflektUsageMap: Map<File, Set<File>>
        get() = fileToReflektUsage

    override fun report(fileSearchedByReflect: File, reflektUsageFile: File) {
        fileToReflektUsage.getOrPut(fileSearchedByReflect) { hashSetOf() }.add(reflektUsageFile)
    }
}