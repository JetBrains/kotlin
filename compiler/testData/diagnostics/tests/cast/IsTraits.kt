// RUN_PIPELINE_TILL: BACKEND
interface Aaa
interface Bbb

fun f(a: Aaa) = a is Bbb

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, isExpression */
