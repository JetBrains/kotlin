// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect open class A() {
    fun member(a: String, b: Int = 0, c: Double? = null): String
}

expect class B() : A

// MODULE: platform()()(common)
// FILE: platform.kt

import kotlin.test.assertEquals

actual open class A actual constructor() {
    actual fun member(a: String, b: Int, c: Double?): String = a + "," + b + "," + c
}

actual class B actual constructor() : A()

fun box(): String {
    val b = B()
    assertEquals("OK,0,null", b.member("OK"))
    assertEquals("OK,42,null", b.member("OK", 42))
    assertEquals("OK,42,3.14", b.member("OK", 42, 3.14))

    return "OK"
}
