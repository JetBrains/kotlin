// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface Tr
interface G<T>

fun test(tr: Tr) = tr is <!NO_TYPE_ARGUMENTS_ON_RHS!>G<!>

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, isExpression, nullableType, typeParameter */
