// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// ISSUE: KT-57181
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect fun <T> topLevel(a: T, b: (T) -> Int = { 1 }): String

expect class Foo() {
    fun <T> member(a: T, b: (T) -> Int = { 2 }): String
}

expect class Bar<T>() {
    fun member(a: T, b: (T) -> Int = { 3 }): String
}

expect class A<T> {
    inner class B<N> {
        fun <H> foo(t: T, n: N, h: H, a: (T, N, H) -> Int = { _, _, _ -> 4 }): String
    }
}

// MODULE: main()()(common)
// FILE: main.kt

import kotlin.test.assertEquals

actual fun <T> topLevel(a: T, b: (T) -> Int): String = b(a).toString()

actual class Foo actual constructor() {
    actual fun <T> member(a: T, b: (T) -> Int): String = b(a).toString()
}

actual class Bar<T> actual constructor() {
    actual fun member(a: T, b: (T) -> Int): String = b(a).toString()
}

actual class A<T> {
    actual inner class B<N> {
        actual fun <H> foo(t: T, n: N, h: H, a: (T, N, H) -> Int) = a(t, n, h).toString()
    }
}

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
