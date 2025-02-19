/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

import kotlin.test.*

class A {
    lateinit var s: String

    fun foo() = s
}

val sb = StringBuilder()

fun box(): String {
    val a = A()
    try {
        sb.appendLine(a.foo())
    }
    catch (e: RuntimeException) {
        sb.append("OK")
        return sb.toString()
    }
    return "Fail"
}