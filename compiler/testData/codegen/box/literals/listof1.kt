/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// This test verifies absence of buggy optimzation removed in https://github.com/JetBrains/kotlin/commit/d222e9587

import kotlin.test.*

fun box(): String {
    val list = foo()
    if (!(list == foo()))
        return "FAIL =="
    if (list === foo())
        return "FAIL ===: two listOf() having same contents are not optimized to be referentially equal in all backends, but are now unexpectedly referentially equal"
    if (list.toString() != "[a, b, c]")
        return "FAIL toString(): ${list.toString()}"

    return "OK"
}

fun foo(): List<String> {
    return listOf("a", "b", "c")
}
