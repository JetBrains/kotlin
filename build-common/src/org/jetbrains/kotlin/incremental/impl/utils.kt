/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import org.jetbrains.kotlin.incremental.md5

fun ByteArray.hashToLong(): Long {
    // Note: The returned type `Long` is 64-bit, but we currently don't have a good 64-bit hash function.
    // The method below uses `md5` which is 128-bit and converts it to `Long`.
    return md5()
}
