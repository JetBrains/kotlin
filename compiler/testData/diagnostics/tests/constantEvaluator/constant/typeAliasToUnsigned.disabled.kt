// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: IntrinsicConstEvaluation
// DISABLE_NEXT_PHASE_SUGGESTION: fails at FRONTEND when the language feature is disabled
// WITH_STDLIB

typealias UI = UInt

const val a: UI = 1u
const val b: UI = a
const val c = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>a<!> == b

/* GENERATED_FIR_TAGS: const, equalityExpression, propertyDeclaration, typeAliasDeclaration, unsignedLiteral */
