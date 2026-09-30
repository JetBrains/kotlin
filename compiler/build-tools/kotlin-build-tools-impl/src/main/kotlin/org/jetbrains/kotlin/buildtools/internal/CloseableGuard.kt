/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package org.jetbrains.kotlin.buildtools.internal

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class CloseableGuard(private val owner: Any) {
    internal val isClosed: AtomicBoolean = AtomicBoolean(false)

    fun close(cleanup: () -> Unit) {
        if (isClosed.compareAndSet(expectedValue = false, newValue = true)) {
            cleanup()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun requireNotClosed() {
        check(!isClosed.load()) { "Cannot perform operation: Resource ${owner::class} was already closed." }
    }
}

