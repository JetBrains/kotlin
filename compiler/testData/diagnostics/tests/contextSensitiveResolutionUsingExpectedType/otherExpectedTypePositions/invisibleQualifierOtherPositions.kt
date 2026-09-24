// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86093

// FILE: privates.kt
package pkg

private object B

class C {
    private companion object
}

// FILE: main.kt
import pkg.*

enum class E {
    B, C
}

fun takesE(e: E) {}

fun test() {
    takesE(<!ARGUMENT_TYPE_MISMATCH!>if (true) <!INVISIBLE_REFERENCE!>B<!> else E.C<!>)
    takesE(<!ARGUMENT_TYPE_MISMATCH!><!INVISIBLE_REFERENCE!>B<!> <!USELESS_ELVIS!>?: E.C<!><!>)
    takesE(<!ARGUMENT_TYPE_MISMATCH!>when { true -> <!INVISIBLE_REFERENCE!>B<!>; else -> <!INVISIBLE_REFERENCE!>C<!> }<!>)
    if (<!EQUALITY_NOT_APPLICABLE!><!INVISIBLE_REFERENCE!>B<!> == E.B<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE!>E.B == <!INVISIBLE_REFERENCE!>B<!><!>) {}
    val e: E = E.B
    <!NO_ELSE_IN_WHEN!>when<!> (e) {
        <!INCOMPATIBLE_TYPES, INVISIBLE_REFERENCE!>B<!> -> {}
        <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY, INCOMPATIBLE_TYPES, INVISIBLE_REFERENCE!>C<!> -> {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, elvisExpression, enumDeclaration, enumEntry,
equalityExpression, functionDeclaration, ifExpression, localProperty, objectDeclaration, propertyDeclaration, smartcast,
whenExpression, whenWithSubject */
