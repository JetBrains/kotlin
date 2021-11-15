// With FIR the annotation implementation class is not regenerated as it
// is seen as coming from the same module.
// See IrSourceCompilerForInline.kt:isCallInsideSameModuleAsCallee.
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR
// IGNORE_DEXING
// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// MODULE: lib
// FILE: lib.kt

package a

annotation class A(val i: Int)

inline fun foo(i: Int): A = A(i)

inline fun bar(f: () -> Int): A = A(f())

// MODULE: app(lib)
// FILE: app.kt

package test

import a.*

class C {
    fun one(): A = foo(1)
    fun two(): A = bar { 2 }
}

fun box(): String {
    val one = C().one()
    val two = C().two()
    assert(one.i == 1)
    assert(two.i == 2)
    // Just like SAM wrappers, annotation implementation classes should be copied from inline functions
    // into current module to avoid compatibility problems when inline fun implementation in origin module
    // has changed (e.g. do not instantiate annotation anymore)
    assert(one.javaClass.getEnclosingClass().getName() == "test.C")
    assert(two.javaClass.getEnclosingClass().getName() == "test.C")
    return "OK"
}
