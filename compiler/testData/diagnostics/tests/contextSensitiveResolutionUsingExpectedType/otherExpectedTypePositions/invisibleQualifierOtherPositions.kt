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
    takesE(if (true) B else E.C)
    takesE(B <!USELESS_ELVIS!>?: E.C<!>)
    takesE(when { true -> B; else -> C })
    if (<!EQUALITY_NOT_APPLICABLE!><!INVISIBLE_REFERENCE!>B<!> == E.B<!>) {}
    if (E.B == B) {}
    val e: E = E.B
    when (e) {
        B -> {}
        C -> {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, elvisExpression, enumDeclaration, enumEntry,
equalityExpression, functionDeclaration, ifExpression, localProperty, objectDeclaration, propertyDeclaration, smartcast,
whenExpression, whenWithSubject */
