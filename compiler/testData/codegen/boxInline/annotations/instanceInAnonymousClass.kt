// WITH_STDLIB
// LANGUAGE: +InstantiationOfAnnotationClasses

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
