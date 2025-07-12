/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// FILE: lib.kt

class Z<T>(val x: T)

inline fun<T, R> foo(x: T, f: (T) -> R): R {
    return f(x)
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    val arr = Array(1) { foo(it, ::Z) }
    assertEquals(0, arr[0].x)
    return "OK"
}