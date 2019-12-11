// !LANGUAGE: +MultiPlatformProjects, +InlineClasses
// MODULE: m1-common
// FILE: common.kt

expect inline class Foo1(val x: Int) {
    fun bar(): String
}

expect inline class Foo2(val x: Int)

expect inline class Foo3

expect class NonInlineExpect

expect inline class NonInlineActual(val x: Int)

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

actual inline class Foo1(val x: Int) {
    actual fun bar(): String = "Hello"
}
actual inline class Foo2(val x: String)
actual inline class Foo3

actual inline class NonInlineExpect(val x: Int)

actual class NonInlineActual actual constructor(actual val x: Int)
