// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: commonMain.kt

expect class A() {
    fun foo(s: String): String

    val bar: String
}

fun test(s: String): String {
    val a = A()
    return a.foo(s) + a.bar
}

// MODULE: platform()()(common)
// FILE: platform.kt

open class B {
    fun foo(s: String) = s

    val bar: String = "K"
}

actual class A : B()

fun box() = test("O")