// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, WASM
// IGNORE_NATIVE_K1: mode=ONE_STAGE_MULTI_MODULE
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-62671

// MODULE: common
// FILE: common.kt
interface A {
    fun foo(x: Int = 1): String
}

class B : A  {
    override fun foo(x: Int): String {
        return if (x == 1) "OK" else "Fail: $x"
    }
}

class X(val delegate: A = B()) : A by delegate

// MODULE: platform()()(common)
// FILE: platform.kt
fun box(): String {
    val x = X()
    return x.foo()
}
