/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

fun box(): String {
    val x = foo()
    if (x.toString() != "kotlin.Unit") return "FAIL: $x"
    return "OK"
}

fun foo() {
    return Unit
}