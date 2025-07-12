/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// FILE: lib.kt

@Suppress("NOTHING_TO_INLINE")
inline fun bar(block: () -> String) : String {
    return block()
}

// FILE: main.kt
import kotlin.test.*

fun bar2() : String {
    return bar { return "OK" }
}

fun box(): String {
    return bar2()
}
