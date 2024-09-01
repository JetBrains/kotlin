// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-24047

// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun f(): Foo?
    fun f(x: Int): Foo?
    fun f(x: Double, y: Int): Foo?
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo {
    actual fun f() = f(-1)
    actual fun f(x: Int): Foo? = null
    actual fun f(x: Double, y: Int): Foo? = null
}
