// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +SoundSmartCastsAfterTry

fun foo() {
    var s: String?
    s = "Test"
    try {
        s = "Other"
    } catch (ex: Exception) {}
    // Problem: here we do not see that 's' is always not-null
    s.hashCode()
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral, tryExpression */
