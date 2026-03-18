// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-18340

// KT-18340: False negative IMPLICIT_CAST_TO_ANY with elvis operator

val g = if (true) 1 else 2.0

val h = (null as String?) ?: 1

/* GENERATED_FIR_TAGS: asExpression, elvisExpression, ifExpression, integerLiteral, intersectionType, nullableType,
propertyDeclaration */
