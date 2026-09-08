// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: IntrinsicConstEvaluation
const val c = 1u + 2u

fun box() = when {
    c != 3u -> "fail"
    else -> "OK"
}

/* GENERATED_FIR_TAGS: const, equalityExpression, functionDeclaration, propertyDeclaration, stringLiteral,
unsignedLiteral, whenExpression */
