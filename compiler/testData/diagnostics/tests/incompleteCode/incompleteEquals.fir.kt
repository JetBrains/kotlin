// RUN_PIPELINE_TILL: FRONTEND
package a

fun foo(a: Any) = a ==<!SYNTAX!><!>

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration */
