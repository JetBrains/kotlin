// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT

fun main() {
    null + <!SYNTAX!>$foo<!>.<!SYNTAX!>$bar<!>.<!SYNTAX!><!>
}

fun foo2() {
    null + <!SYNTAX!>$foo<!>. <!SYNTAX!>$bar<!> . <!SYNTAX!>$baz<!> .<!SYNTAX!><!>
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration */
