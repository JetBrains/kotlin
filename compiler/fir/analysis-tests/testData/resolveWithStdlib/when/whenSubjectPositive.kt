// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun size(x: String?): String =
    when (val len = x?.length) {
        null -> "none"
        in 0..3 -> "short"
        else -> "long"
    }

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, rangeExpression, safeCall, smartcast, stringLiteral, whenExpression, whenWithSubject */
