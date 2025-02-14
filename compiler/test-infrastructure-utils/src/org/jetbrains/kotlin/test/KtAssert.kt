/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


/*
 * Those functions are needed only in modules which are not depend on any testing framework
 */
object KtAssert {
    @JvmStatic
    fun fail(message: String): Nothing {
        throw AssertionError(message)
    }

    @JvmStatic
    @OptIn(ExperimentalContracts::class)
    fun assertNotNull(message: String, value: Any?) {
        contract {
            returns() implies (value != null)
        }
        if (value == null) {
            fail(message)
        }
    }

    @JvmStatic
    @OptIn(ExperimentalContracts::class)
    fun assertNull(message: String, value: Any?) {
        contract {
            returns() implies (value == null)
        }
        if (value != null) {
            fail(message)
        }
    }

    @JvmStatic
    fun assertTrue(message: String, value: Boolean) {
        if (!value) {
            fail(message)
        }
    }

    @JvmStatic
    fun <T> assertEquals(message: String, expected: T, actual: T) {
        if (expected != actual) {
            fail(message)
        }
    }
}
