// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_IR_AGAINST_OLD

// (supported: JVM_IR, JS_IR(_E6))

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// FILE: 1.kt

package a

annotation class A(val i: Int)

inline fun foo(i: Int): A = A(i)

inline fun bar(f: () -> Int): A = A(f())

// FILE: 2.kt

import a.*
import kotlin.test.assertTrue as assert

class C {
    fun one(): A {
        return foo(1)
    }
}

fun two(): A {
    return bar { 2 }
}

fun box(): String {
    val one = C().one()
    assert(one.i == 1)
    val two = two()
    assert(two.i == 2)
    return "OK"
}
