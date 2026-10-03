/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.SerializedKlibFingerprint
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.klibAttribute

/**
 * Lazily evaluated [Klib]'s attribute that keeps the library's fingerprint cache.
 */
val Klib.lazyEvaluatedFingerprintHash: FingerprintHash
    get() {
        cachedFingerprintHash?.let { return it }

        val hash = SerializedKlibFingerprint(path.toFile()).klibFingerprint
        cachedFingerprintHash = hash
        return hash
    }

private var Klib.cachedFingerprintHash: FingerprintHash? by klibAttribute()
