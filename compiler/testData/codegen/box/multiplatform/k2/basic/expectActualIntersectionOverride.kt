// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE
// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58124

// MODULE: common
// FILE: common.kt

interface I1 {
    fun f(): String
}

interface I2 {
    fun f(): String
}

expect class C() : I1, I2

fun test() = C().f()

// MODULE: platform()()(common)
// FILE: platform.kt

actual class C : I1, I2 {
    override fun f() = "OK"
}

fun box() = test()