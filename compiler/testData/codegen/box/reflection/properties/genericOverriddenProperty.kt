// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// ^ returnType is not supported

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
