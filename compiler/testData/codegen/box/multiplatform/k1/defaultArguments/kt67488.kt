// LANGUAGE: +MultiPlatformProjects

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
