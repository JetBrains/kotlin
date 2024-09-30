// LANGUAGE: +MultiPlatformProjects

// IGNORE_BACKEND_K1: JVM
// The legacy JVM backend throws java.lang.InstantiationError

// IGNORE_BACKEND_K2: ANY
// FIR status: outdated code (expect and actual in the same module)

// FILE: common.kt
expect annotation class A()

fun foo(@A x: Int) = "OK"

fun bar() = A()

// FILE: platform.kt
actual annotation class A(val value: String = "OK")

fun box(): String {
    foo(42).let {
        if (it != "OK") return it
    }

    bar().value.let {
        if (it != "OK") return it
    }

    return "OK"
}
