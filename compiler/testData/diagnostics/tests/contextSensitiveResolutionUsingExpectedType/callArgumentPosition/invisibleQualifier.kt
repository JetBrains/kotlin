// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86093

// FILE: privates.kt
package pkg

private object B
class C {
    private companion object
}
private object Aliased

// FILE: main.kt
import pkg.*
import pkg.<!INVISIBLE_REFERENCE!>Aliased<!> as X

enum class E {
    B, C, X
}

fun takesE(e: E) {}
fun takesInt(i: Int) {}
fun overloaded(e: E, i: Int) {}
fun overloaded(e: E, s: String) {}

fun test() {
    takesE(B)
    takesE(C)
    takesE(X)
    overloaded(B, 1)
    overloaded(C, "")
}

fun negative() {
    takesInt(<!ARGUMENT_TYPE_MISMATCH, INVISIBLE_REFERENCE!>B<!>)
    takesInt(<!ARGUMENT_TYPE_MISMATCH, INVISIBLE_REFERENCE!>C<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration,
integerLiteral, objectDeclaration, stringLiteral */
