// RUN_PIPELINE_TILL: FRONTEND
class X : <!UNRESOLVED_REFERENCE!>S<!>

fun f(l: List<X>) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
