/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

fun box(): String {
    val actual = foo(Unit)
    if (actual != "kotlin.Unit") return "FAIL: $actual"
    return "OK"
}

fun foo(x: Any): String {
    return x.toString()
}