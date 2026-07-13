// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE_FEATURE_TOGGLED: IntrinsicConstEvaluation
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL
// WITH_STDLIB

const val charLiteralCode = 'c'.code

const val charConst = 'c'
const val charConstCode = charConst.code

/* GENERATED_FIR_TAGS: const, propertyDeclaration */
