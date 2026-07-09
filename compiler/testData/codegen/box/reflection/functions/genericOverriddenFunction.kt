// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// ^ returnType is not supported

// WITH_REFLECT

package test

import kotlin.test.assertEquals

interface H<T> {
    fun foo(): T?
}

interface A : H<A>

fun box(): String {
    assertEquals("test.A?", A::foo.returnType.toString())
    assertEquals("T?", H<A>::foo.returnType.toString())

    return "OK"
}
