// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, WASM
// IGNORE_NATIVE_K1: mode=ONE_STAGE_MULTI_MODULE
// MODULE: common
// FILE: common.kt
expect class A {
    class B {
        fun foo()
    }
}

// MODULE: main()()(common)
// FILE: test.kt
actual class A {
    actual class B {
        actual fun foo() {}
    }
}

fun box() = "OK" // check no errors are thrown during building FIR member mapping
