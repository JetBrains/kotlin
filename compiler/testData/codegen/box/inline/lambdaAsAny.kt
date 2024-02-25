/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

inline fun foo(x: Any) {
    sb.append(if (x === x) "OK" else "FAIL")
}

fun box(): String {
    foo { 42 }

    return sb.toString()
}