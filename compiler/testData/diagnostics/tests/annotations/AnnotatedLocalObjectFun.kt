// RUN_PIPELINE_TILL: CODEGEN
annotation class My

fun foo() {
    val s = object {
        @My fun bar() {}
    }
    s.bar()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, functionDeclaration, localProperty,
propertyDeclaration */
