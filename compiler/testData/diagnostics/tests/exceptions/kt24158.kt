// RUN_PIPELINE_TILL: FRONTEND

fun main() {
    null + <!SYNTAX!>$foo<!>.<!SYNTAX!>$bar<!>.<!SYNTAX!><!>
}

fun foo2() {
    null + <!SYNTAX!>$foo<!>. <!SYNTAX!>$bar<!> . <!SYNTAX!>$baz<!> .<!SYNTAX!><!>
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration */
