/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB

// FILE: lib.kt

fun interface Foo: suspend () -> Unit

// FILE: main.kt
import kotlin.test.*

val foo = Foo {}

fun box(): String {
    assertEquals(foo, foo) // Circumvent DCE.

    return "OK"
}
