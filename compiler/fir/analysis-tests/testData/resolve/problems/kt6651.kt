// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-6651

// KT-6651: Inline method with Unit return type compilation error

fun foo() {
    bar({var a = 3})
}

fun bar(t: Function0<*>) {
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration,
starProjection */
