// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: IntrinsicConstEvaluation
// WITH_STDLIB

const val c = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u + 2u<!>

/* GENERATED_FIR_TAGS: additiveExpression, const, propertyDeclaration, unsignedLiteral */
