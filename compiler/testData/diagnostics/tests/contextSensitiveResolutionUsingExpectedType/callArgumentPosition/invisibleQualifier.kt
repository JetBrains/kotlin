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
    takesE(<!ARGUMENT_TYPE_MISMATCH, INVISIBLE_REFERENCE!>B<!>)
    takesE(<!ARGUMENT_TYPE_MISMATCH, INVISIBLE_REFERENCE!>C<!>)
    takesE(<!ARGUMENT_TYPE_MISMATCH, INVISIBLE_REFERENCE!>X<!>)
    <!NONE_APPLICABLE!>overloaded<!>(<!ARGUMENT_TYPE_MISMATCH, INVISIBLE_REFERENCE!>B<!>, 1)
    <!NONE_APPLICABLE!>overloaded<!>(<!ARGUMENT_TYPE_MISMATCH, INVISIBLE_REFERENCE!>C<!>, "")
}

fun negative() {
    takesInt(<!ARGUMENT_TYPE_MISMATCH, INVISIBLE_REFERENCE!>B<!>)
    takesInt(<!ARGUMENT_TYPE_MISMATCH, INVISIBLE_REFERENCE!>C<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration,
integerLiteral, objectDeclaration, stringLiteral */
