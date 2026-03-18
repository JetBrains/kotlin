// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-17021
// WITH_STDLIB

// KT-17021: IMPLICIT_CAST_TO_ANY isn't reported for lambda result
fun someCondition(): Boolean = true

val f = {
    // No warning here - this is the bug (should also warn)
    if (someCondition()) {
        42
    } else {

    }
}

val g = {
    // IMPLICIT_CAST_TO_ANY is reported here
    val x = if (someCondition()) {
        42
    } else {

    }
    x
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration */
