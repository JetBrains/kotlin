/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt

val sb = StringBuilder()

inline fun foo(x: Any) {
    sb.append(if (x === x) "OK" else "FAIL")
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    foo { 42 }

    return sb.toString()
}