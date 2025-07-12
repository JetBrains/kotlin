/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// FILE: lib.kt

val sb = StringBuilder()

inline fun bar() {
    sb.append({ "OK" }())
}

// FILE: main.kt
class Foo {
    init {
        bar()
    }
}

fun box(): String {
    Foo()
    return sb.toString()
}