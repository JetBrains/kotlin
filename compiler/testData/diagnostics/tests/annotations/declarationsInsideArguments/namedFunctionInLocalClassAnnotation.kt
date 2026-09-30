// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041

annotation class Anno(val i: Int)

fun test() {
    @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
    class Local

    val obj = @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1) object {}
}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, classDeclaration, functionDeclaration,
integerLiteral, localClass, localFunction, localProperty, primaryConstructor, propertyDeclaration */
