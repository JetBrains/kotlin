// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: IntrinsicConstEvaluation
// DISABLE_NEXT_PHASE_SUGGESTION: fails at FRONTEND when the language feature is disabled
const val c = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u + 2u<!>

fun box() = when {
    c != 3u -> "fail"
    else -> "OK"
}

/* GENERATED_FIR_TAGS: const, equalityExpression, functionDeclaration, propertyDeclaration, stringLiteral,
unsignedLiteral, whenExpression */
