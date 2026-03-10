/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.CompanionBlockInfo

class CompanionBlockCollector {
    val validBlocks: MutableList<KtSourceElement> = mutableListOf()
    val nestedBlocks: MutableList<KtSourceElement> = mutableListOf()

    fun collect(source: KtSourceElement, isNested: Boolean) {
        if (isNested) {
            nestedBlocks
        } else {
            validBlocks
        }.add(source)
    }

    fun toCompanionBlockInfoOrNull(): CompanionBlockInfo? {
        if (validBlocks.isEmpty()) return null
        return CompanionBlockInfo(validBlocks, nestedBlocks)
    }
}