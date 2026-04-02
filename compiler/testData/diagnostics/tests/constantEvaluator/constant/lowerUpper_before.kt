// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -IntrinsicConstEvaluation
// WITH_STDLIB

const val lower1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"hEllO".lowercase()<!>
const val upper1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"World".uppercase()<!>

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
