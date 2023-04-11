// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE
// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-57984

// MODULE: common
// FILE: common.kt

expect interface I {
    fun ok(): String
}
interface I2 : I

fun ok(c: I2) = c.ok()

// MODULE: lib()()(common)
// FILE: lib.kt

actual interface I {
    actual fun ok(): String
}

// MODULE: main(lib)
// FILE: main.kt

class C : I2 {
    override fun ok(): String = "OK"
}

fun box() = ok(C())