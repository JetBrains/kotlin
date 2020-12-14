/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KtAssert")

package org.jetbrains.kotlin.test

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/*
 * Those functions are needed only in this module because it has no testing framework
 *   with assertions in it's dependencies
 */

internal fun fail(message: String) {
    throw AssertionError(message)
}

@OptIn(ExperimentalContracts::class)
internal fun assertNotNull(message: String, value: Any?) {
    contract {
        returns() implies (value != null)
    }
    if (value == null) {
        fail(message)
    }
}

internal fun assertTrue(message: String, value: Boolean) {
    if (!value) {
        fail(message)
    }
}
