// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface A
interface B
fun testing(a: A) = a as B

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, interfaceDeclaration */
