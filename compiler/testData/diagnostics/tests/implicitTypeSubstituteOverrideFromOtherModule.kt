// RUN_PIPELINE_TILL: BACKEND
// MODULE: lib
package dependency

abstract class A<T> {
    fun foo() = ""
}

// MODULE: main(lib)
// FILE: B.kt
package main
import c.C
import dependency.A

abstract class B : A<C>() {}

fun usage(b : B?) {
    if (b != null) {
        b.foo()
    }
}
// FILE: C.kt
package c
class C

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, nullableType, smartcast,
stringLiteral, typeParameter */
