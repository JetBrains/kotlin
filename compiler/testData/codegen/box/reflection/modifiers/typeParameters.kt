// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

import kotlin.test.assertTrue
import kotlin.test.assertFalse

class A {
    fun <T> nonReified(): T = null!!
    inline fun <reified U> reified(): U = null!!
}

fun box(): String {
    assertFalse(A::class.members.single { it.name == "nonReified" }.typeParameters.single().isReified)
    assertTrue(A::class.members.single { it.name == "reified" }.typeParameters.single().isReified)
    return "OK"
}
