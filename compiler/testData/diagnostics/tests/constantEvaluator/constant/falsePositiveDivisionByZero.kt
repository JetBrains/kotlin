// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
const val x = 1L / (1L shl 32)
const val y = 1UL / (1UL shl 32)

/* GENERATED_FIR_TAGS: const, integerLiteral, multiplicativeExpression, propertyDeclaration */
