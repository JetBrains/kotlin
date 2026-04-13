// RUN_PIPELINE_TILL: FRONTEND
fun testing(a: Any) = a is <!UNRESOLVED_REFERENCE!>UnresolvedType<!><Int>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression */
