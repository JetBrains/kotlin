// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE, WASM
// FIR status: outdated code (expect/actual in the same module)
// WITH_STDLIB
// MODULE: lib
// FILE: common.kt

expect fun <T> topLevel(a: T, b: (T) -> Int = { 1 }): String

actual fun <T> topLevel(a: T, b: (T) -> Int): String = b(a).toString()

expect class Foo() {
    fun <T> member(a: T, b: (T) -> Int = { 2 }): String
}

actual class Foo actual constructor() {
    actual fun <T> member(a: T, b: (T) -> Int): String = b(a).toString()
}

expect class Bar<T>() {
    fun member(a: T, b: (T) -> Int = { 3 }): String
}

actual class Bar<T> actual constructor() {
    actual fun member(a: T, b: (T) -> Int): String = b(a).toString()
}

expect class A<T> {
    inner class B<N> {
        fun <H> foo(t: T, n: N, h: H, a: (T, N, H) -> Int = { _, _, _ -> 4 }): String
    }
}

actual class A<T> {
    actual inner class B<N> {
        actual fun <H> foo(t: T, n: N, h: H, a: (T, N, H) -> Int) = a(t, n, h).toString()
    }
}

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("1", topLevel("OK"))
    assertEquals("73", topLevel("OK") { 73 })

    val foo = Foo()
    assertEquals("2", foo.member("OK"))
    assertEquals("42", foo.member("OK") { 42 })

    val bar = Bar<String>()
    assertEquals("3", bar.member("OK"))
    assertEquals("37", bar.member("OK") { 37 })

    val b = A<Int>().B<Double>()
    assertEquals("4", b.foo<Int>(1, 2.0, 3))
    assertEquals("6", b.foo<Int>(1, 2.0, 3) { t, n, h -> t + n.toInt() + h })

    return "OK"
}
