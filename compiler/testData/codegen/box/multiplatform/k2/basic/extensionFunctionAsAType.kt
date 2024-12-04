// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

package test

expect class A {
    fun foo(a: Int.() -> String): Int.() -> String
}

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual class A {
    actual fun foo(a: Int.() -> String): Int.() -> String = a
}

fun box() = A().foo{ "OK" } (1)