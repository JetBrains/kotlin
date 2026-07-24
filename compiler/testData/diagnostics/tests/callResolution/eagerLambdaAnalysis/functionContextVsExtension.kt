// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-87161

context(a: () -> String)
fun f() = 1

fun (() -> Unit).f() = "2"

fun test() {
    with({ "" }) {
        val result = f()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
    }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, functionalType,
integerLiteral, lambdaLiteral, localProperty, propertyDeclaration, stringLiteral */
