// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect open class C() {
    open fun f(p: Int = 2) : String
}

// MODULE: platform()()(common)
// FILE: platform.kt

import kotlin.test.assertEquals

actual open class C {
    actual open fun f(p: Int) = "C" + p
}

open class D : C() {
    override open fun f(p: Int) = "D" + p
}

fun box(): String {

    assertEquals("C2", C().f())
    assertEquals("C9", C().f(9))
    assertEquals("D2", D().f())
    assertEquals("D5", D().f(5))

    return "OK"
}