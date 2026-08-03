// RUN_PIPELINE_TILL: BACKEND
// MODULE: lib
package dependency

abstract class A {
    abstract fun foo()
}
interface I {
    fun foo()
}

// MODULE: main(lib)
// FILE: B.kt
package main
import dependency.A
import dependency.I

abstract class B : A(), I {}

fun usage(b : B?) {
    if (b != null) {
        b.foo()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration,
nullableType, smartcast */
