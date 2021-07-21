// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_MULTI_MODULE_OLD_AGAINST_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD
// WITH_RUNTIME
// !LANGUAGE: +InstantiationOfAnnotationClasses
// IGNORE_DEXING
// TODO: D8 fails with AssertionError and does not print reason, need further investigation

// FILE: 1.kt

package a

annotation class A(val i: Int)

inline fun foo(i: Int): A = A(i)

inline fun bar(f: () -> Int): A = A(f())

// FILE: 2.kt

import a.*

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
    // During cross-module inlining, anonymous classes are copied
    // println(one.javaClass.getName().startsWith("a._1Kt"))
    return "OK"
}