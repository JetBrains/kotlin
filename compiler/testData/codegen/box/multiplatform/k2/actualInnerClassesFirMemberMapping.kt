// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: ANY
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
