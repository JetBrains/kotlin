/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import com.google.common.hash.Hashing


private val fingerprint = Hashing.farmHashFingerprint64()

fun ByteArray.hashToLong(): Long {
    // FarmHashFingerprint64 produces a 64-bit fingerprint represented as a `Long`.
    return fingerprint.hashBytes(this).asLong()
}
