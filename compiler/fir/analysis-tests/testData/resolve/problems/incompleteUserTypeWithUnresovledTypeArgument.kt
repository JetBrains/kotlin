// RUN_PIPELINE_TILL: FRONTEND
class A<T>

fun foo(a : A<<!UNRESOLVED_REFERENCE!>Unresolved<!>>.<!SYNTAX!><!>) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeParameter */
