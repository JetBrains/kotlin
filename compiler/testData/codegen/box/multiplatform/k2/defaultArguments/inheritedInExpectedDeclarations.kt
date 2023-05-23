// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect open class A() {
    open fun f(p: Int = 1) : String
}

expect open class B : A {
    override open fun f(p: Int) : String
}

// MODULE: platform()()(common)
// FILE: platform.kt

import kotlin.test.assertEquals

actual open class A {
    actual open fun f(p: Int) = "A" + p
}

actual open class B : A() {
    actual override open fun f(p: Int) = "B" + p
}

fun box(): String {

    assertEquals("A1", A().f())
    assertEquals("A9", A().f(9))
    assertEquals("B1", B().f())
    assertEquals("B5", B().f(5))

    return "OK"
}