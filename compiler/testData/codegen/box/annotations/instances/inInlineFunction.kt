// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// DONT_TARGET_EXACT_BACKEND: JS
// IGNORE_DEXING

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

import kotlin.test.*

annotation class A(val i: Int)

inline fun foo(i: Int): A = A(i)

inline fun bar(f: () -> Int): A = A(f())

class C {
    fun one(): A = foo(1)
    fun two(): A = bar { 2 }
}

fun box(): String {
    val one = C().one()
    val two = C().two()
    assertEquals(1, one.i)
    assertEquals(2, two.i)
    assertEquals(A(1), one)
    assertEquals(A(2), two)
    return "OK"
}
