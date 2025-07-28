/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// TODO replace usages with stdlib functions and remove when KT-70247 is fixed.

internal inline fun <T, R> context(with: T, block: context(T) () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(with)
}

internal inline fun <A, B, Result> context(a: A, b: B, block: context(A, B) () -> Result): Result {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(a, b)
}

internal inline fun <A, B, C, Result> context(a: A, b: B, c: C, block: context(A, B, C) () -> Result): Result {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(a, b, c)
}