/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test

import kotlin.test.*

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    if (expected != null && actual != null) {
        assertEquals(expected::class, actual::class)
    } else {
        assertTrue(expected == null && actual == null)
    }
}

public actual fun randomInt(limit: Int): Int = kotlin.random.Random.nextInt(limit)

internal actual fun String.removeLeadingPlusOnJava6(): String = this
