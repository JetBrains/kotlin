// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-87117

val <T> (() -> T).bar: Int
    get() = 1

val (() -> Unit).bar: String
    get() = "2"

fun <T> test(a: T) {
    val result = { a }.bar
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, getter, integerLiteral, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral, typeParameter */
