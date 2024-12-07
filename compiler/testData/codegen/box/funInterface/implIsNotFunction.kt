/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB

import kotlin.test.*

fun interface Foo {
    fun invoke(): String
}

fun foo(f: Foo) = f is Function<*>

fun box(): String {
    assertFalse(foo { "zzz" })
    return "OK"
}