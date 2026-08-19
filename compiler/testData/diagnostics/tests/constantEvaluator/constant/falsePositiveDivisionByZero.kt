// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
const val x = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DIVISION_BY_ZERO!>1L / (1L shl 32)<!>
const val y = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1UL / (1UL shl 32)<!>

/* GENERATED_FIR_TAGS: const, integerLiteral, multiplicativeExpression, propertyDeclaration */
