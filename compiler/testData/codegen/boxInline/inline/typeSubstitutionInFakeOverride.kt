/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// FILE: lib.kt

abstract class A {
    inline fun <reified T : Any> baz(): String {
        return T::class.simpleName!!
    }
}

// FILE: main.kt
class B : A() {
    fun bar(): String {
        return baz<OK>()
    }
}

class OK

fun box(): String {
    return B().bar()
}