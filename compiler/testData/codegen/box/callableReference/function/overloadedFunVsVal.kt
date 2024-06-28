// IGNORE_BACKEND_K1: WASM
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

import kotlin.reflect.*

class A {
    val x = 1
    fun x(): String = "OK"
}

val f1: KProperty1<A, Int> = A::x
val f2: (A) -> String = A::x

fun box(): String {
    val a = A()

    val x1 = f1.get(a)
    if (x1 != 1) return "Fail 1: $x1"

    return f2(a)
}
