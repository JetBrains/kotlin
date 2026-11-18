// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.createInstance

value class A(val x: Int = 1, val y: Int = 2)
value class B(val x: Int = 3)
inline value class C(val x: Int = 4)

fun box(): String {
    require(A::class.createInstance().x == 1) { "A().x == 1" }
    require(A::class.createInstance().y == 2) { "A().y == 2" }
    // A single-field full value class is treated like an inline class, for which `createInstance` does not work (KT-85456)
    try {
        B::class.createInstance()
        return "Fail: B::class.createInstance() is expected to throw"
    } catch (_: IllegalArgumentException) {
    }
    // `createInstance` does not work for an inline class (KT-85456)
    try {
        C::class.createInstance()
        return "Fail: C::class.createInstance() is expected to throw"
    } catch (_: IllegalArgumentException) {
    }

    return "OK"
}
