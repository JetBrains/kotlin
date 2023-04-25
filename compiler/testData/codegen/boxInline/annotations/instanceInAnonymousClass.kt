// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_IR_AGAINST_OLD

// (supported: JVM_IR, JS_IR(_E6))

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// FILE: 1.kt

package a

annotation class A(val i: String)

interface I {
    fun g(): A
}

inline fun foo(i: String): I = object : I {
    override fun g(): A {
        return A(i)
    }
}

// FILE: 2.kt

import a.*

class C() {
    fun one(): A = foo("OK").g()
}

fun box(): String {
    return C().one().i
}
