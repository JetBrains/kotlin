/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import java.util.*

fun BitSet.copy() = BitSet(this.size()).apply { this.or(this@copy) }

inline fun BitSet.forEachBit(block: (Int) -> Unit) {
    var i = -1
    while (true) {
        i = nextSetBit(i + 1)
        if (i < 0) break
        block(i)
    }
}
