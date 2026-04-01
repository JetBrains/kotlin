// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-31236
// FIR_DUMP

// KT-31236: Prohibit implicit intersection types as return types in new inference
interface A
interface B

fun foo(x: Any?) = if (x is A && x is B) x else null

/* GENERATED_FIR_TAGS: andExpression, functionDeclaration, ifExpression, interfaceDeclaration, intersectionType,
isExpression, nullableType, smartcast */
