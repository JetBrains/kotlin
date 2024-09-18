// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: WASM

// WITH_REFLECT
// KT-13700 Exception obtaining descriptor for property reference

package test

import kotlin.test.assertEquals

interface H<T> {
    val parent : T?
}

interface A : H<A>

fun box(): String {
    assertEquals("test.A?", A::parent.returnType.toString())
    assertEquals("T?", H<A>::parent.returnType.toString())

    return "OK"
}
