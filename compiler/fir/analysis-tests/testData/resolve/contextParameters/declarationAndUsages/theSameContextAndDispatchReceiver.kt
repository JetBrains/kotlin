// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class A {
    context(a: A)
    fun funMember() {}

    context(a: A)
    val propertyMember: String
        get() = ""

    fun usageInsideClass() {
        funMember()
        propertyMember
    }
}

fun usageOutsideClass() {
    with(A()) {
        funMember()
        propertyMember
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, getter, lambdaLiteral,
propertyDeclaration, propertyDeclarationWithContext, stringLiteral */
