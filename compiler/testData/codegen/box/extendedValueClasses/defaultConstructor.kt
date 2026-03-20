// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING
// WORKS_WHEN_VALUE_CLASS
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.createInstance

value class A(val x: Int = 1, val y: Int = 2)
value class B(val x: Int = 3)
OPTIONAL_JVM_INLINE_ANNOTATION
value class C(val x: Int = 4)

fun box(): String {
    require(A::class.createInstance().x == 1) { "A().x == 1" }
    require(A::class.createInstance().y == 2) { "A().y == 2" }
    require(B::class.createInstance().x == 3) { "B().x == 3" }
    require(C::class.createInstance().x == 4) { "C().x == 4" }

    return "OK"
}
