// RUN_PIPELINE_TILL: CODEGEN
interface A
interface B
fun testing(a: A) = a as B

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, interfaceDeclaration */
