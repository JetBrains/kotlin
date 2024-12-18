// LANGUAGE: +MultiPlatformProjects, +ContextParameters

// MODULE: common
// FILE: common.kt

package test

open class Base {
    context(c: String)
    fun foo(): String {
        return c
    }

    context(c: String)
    val a: String
        get() = c
}

expect class A : Base

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual class A : Base()

fun box(): String {
    with(A()) { return with("O") { foo() } + with("K") { a } }
}