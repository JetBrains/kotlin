// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun <T> testing(a: T) = <!USELESS_IS_CHECK!>a is T<!>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, nullableType, typeParameter */
