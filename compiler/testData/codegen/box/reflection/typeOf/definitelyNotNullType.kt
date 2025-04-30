// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// WITH_REFLECT

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class C<T> {
    fun f(): KType = typeOf<List<T & Any>>()
}

fun box(): String {
    // Should be `List<T & Any>` (KT-77299).
    assertEquals("kotlin.collections.List<T>", C<Any>().f().toString())

    return "OK"
}
