// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun foo(block: () -> Unit) = 1
fun foo(block: StringSam) = "(2)"

fun interface StringSam {
   fun run(): String
}

fun bar() = "OK"

fun test() {
   val result = foo(::bar)
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
}

/* GENERATED_FIR_TAGS: callableReference, funInterface, functionDeclaration, functionalType, integerLiteral,
interfaceDeclaration, localProperty, propertyDeclaration, samConversion, stringLiteral */
