// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DUMP_CFG: LEVELS
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Int) = 1

val y = 2

foo(y)

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
