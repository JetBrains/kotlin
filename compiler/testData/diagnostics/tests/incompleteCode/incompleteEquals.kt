// RUN_PIPELINE_TILL: FRONTEND
package a

fun foo(a: Any) = a <!DEBUG_INFO_MISSING_UNRESOLVED!>==<!><!SYNTAX!><!>

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration */
