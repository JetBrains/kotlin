/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.ConstantRef

class InlineConstTrackerImpl : InlineConstTracker {
    private val inlineConst = hashMapOf<String, MutableSet<ConstantRef>>()

    val inlineConstMap: Map<String, Collection<ConstantRef>>
        get() = inlineConst

    override fun report(filePath: String, cRefs: Collection<ConstantRef>) {
        inlineConst.getOrPut(filePath) { hashSetOf() }.addAll(cRefs)
    }
}
