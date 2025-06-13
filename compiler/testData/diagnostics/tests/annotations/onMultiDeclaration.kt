// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun test(): Any? {
    <!WRONG_ANNOTATION_TARGET!>@ann<!> val (a, b) = P(1, 1)
    return a + b
}

annotation class ann
data class P(val a: Int, val b: Int)

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, classDeclaration, data, destructuringDeclaration,
functionDeclaration, integerLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration */
