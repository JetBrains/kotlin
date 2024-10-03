// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt

package test

open class Base {
    fun Int.foo(): String { return "O" }
    val Int.a : String
        get() = "K"
}

expect class A : Base

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual class A : Base()

fun box(): String = with(A()) { 1.foo() + 1.a }