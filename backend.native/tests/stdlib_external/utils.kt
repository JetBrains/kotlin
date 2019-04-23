/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test

import kotlin.test.*

public actual fun assertTypeEquals(expected: Any?, actual: Any?) {
    if (expected != null && actual != null) {
        assertTrue(expected::class.isInstance(actual) || actual::class.isInstance(expected),
                "Expected: $expected,  Actual: $actual")
    } else {
        assertTrue(expected == null && actual == null)
    }
}

internal actual fun String.removeLeadingPlusOnJava6(): String = this

internal actual inline fun testOnNonJvm6And7(f: () -> Unit) {
    f()
}

actual fun testOnJvm(action: () -> Unit) {}
actual fun testOnJs(action: () -> Unit) {}