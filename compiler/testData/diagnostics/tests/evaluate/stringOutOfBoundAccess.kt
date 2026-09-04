// LANGUAGE_FEATURE_TOGGLED_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE_FEATURE_TOGGLED: IntrinsicConstEvaluation

const val s = "abc"
const val getCosnt0 = s[0]
const val getCosnt2 = s[2]
const val getCosnt3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>s[3]<!>
const val getCosntMinus1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>s[-1]<!>

const val getLiteral0 = "string"[0]
const val getLiteral5 = "string"[5]
const val getLiteral6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"string"[6]<!>
const val getLiteral9 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"string"[9]<!>
const val getLiteralMinus1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"string"[-1]<!>

/* GENERATED_FIR_TAGS: const, integerLiteral, propertyDeclaration, stringLiteral */
