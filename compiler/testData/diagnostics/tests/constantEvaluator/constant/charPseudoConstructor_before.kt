// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -IntrinsicConstEvaluation
// WITH_STDLIB

const val char1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Char(65)<!>

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
