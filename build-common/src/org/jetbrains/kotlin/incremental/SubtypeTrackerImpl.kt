/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.SubtypeTracker
import org.jetbrains.kotlin.name.FqName

class SubtypeTrackerImpl : SubtypeTracker {
    private val subtypes = hashMapOf<FqName, MutableSet<FqName>>()

    val subtypeMap: Map<FqName, Set<FqName>>
        get() = subtypes

    override fun report(className: FqName, subtypeName: FqName) {
        subtypes.getOrPut(className) { hashSetOf() }.add(subtypeName)
    }
}
