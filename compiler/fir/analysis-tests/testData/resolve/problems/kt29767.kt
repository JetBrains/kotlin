// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-29767

// KT-29767: No smartcast diagnostic on qualifier expression of captured type
fun case_1(a: MutableList<out Int?>?) {
    if (a != null) {
        val b = a[0] // no SMARTCAST diagnostic
        if (b != null) {
            b.inc() // Inferred to Int
        }
    }
}

/* GENERATED_FIR_TAGS: capturedType, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
localProperty, nullableType, outProjection, propertyDeclaration, smartcast */
