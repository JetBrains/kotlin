// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

const val charLiteralCode = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>'c'.code<!>

const val charConst = 'c'
const val charConstCode = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>charConst.code<!>

/* GENERATED_FIR_TAGS: const, propertyDeclaration */
