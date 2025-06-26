// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78324

interface A<T>
interface B : A<String>
private fun test(): Any = object : A<Int>, B {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, interfaceDeclaration, nullableType, typeParameter */
