// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
annotation class My

fun foo() {
    val s = object {
        @My fun bar() {}
    }
    s.bar()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, functionDeclaration, localProperty,
propertyDeclaration */
