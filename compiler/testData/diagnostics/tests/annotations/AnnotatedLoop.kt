// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class My

fun foo() {
    for (i: @My Int in 0..41) {
        if (i == 13) return
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, equalityExpression, forLoop, functionDeclaration, ifExpression,
integerLiteral, localProperty, propertyDeclaration, rangeExpression */
