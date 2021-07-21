// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_MULTI_MODULE_OLD_AGAINST_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD
// WITH_RUNTIME
// !LANGUAGE: +InstantiationOfAnnotationClasses
// IGNORE_DEXING
// TODO: D8 fails with AssertionError and does not print reason, need further investigation

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