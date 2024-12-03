/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// FILE: 1.kt
inline fun run1(block: String.() -> String): String {
    val tmp1 = block("0") // invocation, converting value param to receiver
    return (tmp1 + "1").block() // usual invocation
}

inline fun run2(block: (String) -> String): String {
    val tmp2 = run1(block) // passing lambda with param as lambda with receiver
    // tmp2.block()  // invocation, converting receiver to value param. Raises `UNRESOLVED_REFERENCE` for both K1 and K2
    return block(tmp2 + "2") // usual invocation
}

inline fun run3(block: String.() -> String): String {
    return run2(block) // passing lambda with receiver as lambda with param
}

// FILE: 2.kt
import kotlin.test.*

fun box(): String {
    val sb2 = StringBuilder()
    val result2 = run2 {
        sb2.append(it + "/")
        it
    }
    assertEquals("012", result2)
    assertEquals("0/01/012/", sb2.toString())

    val sb3 = StringBuilder()
    val result3 = run3 {
        sb3.append(this + "/")
        this
    }
    assertEquals("012", result3)
    assertEquals("0/01/012/", sb3.toString())

    return "OK"
}
