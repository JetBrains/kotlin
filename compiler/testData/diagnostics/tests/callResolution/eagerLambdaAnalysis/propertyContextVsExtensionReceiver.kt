// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-87161

object Type

context(a: () -> Type)
val bar: Int
    get() = 1

val (() -> Unit).bar: String
    get() = "2"

fun test() {
    with({ Type }) {
        val result = bar
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, getter, integerLiteral, lambdaLiteral, localProperty,
objectDeclaration, propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral */
