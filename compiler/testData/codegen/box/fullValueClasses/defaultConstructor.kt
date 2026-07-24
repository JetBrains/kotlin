// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.createInstance

value class A(val x: Int = 1, val y: Int = 2)
value class B(val x: Int = 3)

fun box(): String {
    require(A::class.createInstance().x == 1) { "A().x == 1" }
    require(A::class.createInstance().y == 2) { "A().y == 2" }
    try {
        require(B::class.createInstance().x == 3) { "B().x == 3" }
        error("ISSUE: KT-85456 K/JS createInstance does not work with value classes")
    } catch (_: Throwable) {
    }

    return "OK"
}
