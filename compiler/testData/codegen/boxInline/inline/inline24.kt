/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// FILE: lib.kt

val sb = StringBuilder()
fun myPrintln(s: String): Unit {
    sb.appendLine(s)
}

fun foo() = myPrintln("foo")
fun bar() = myPrintln("bar")

inline fun baz(x: Unit = foo(), y: Unit) {}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    baz(y = bar())

    assertEquals("""
        bar
        foo

    """.trimIndent(), sb.toString())
    return "OK"
}
