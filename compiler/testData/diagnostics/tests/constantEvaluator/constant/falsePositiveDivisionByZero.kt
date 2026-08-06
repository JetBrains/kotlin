// RUN_PIPELINE_TILL: BACKEND
const val x = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DIVISION_BY_ZERO!>1L / (1L shl 32)<!>

/* GENERATED_FIR_TAGS: const, integerLiteral, multiplicativeExpression, propertyDeclaration */
