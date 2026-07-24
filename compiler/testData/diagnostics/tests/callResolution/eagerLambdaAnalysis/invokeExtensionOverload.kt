// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

object Type

operator fun Type.invoke(block: () -> Type): Int = 1

operator fun Type.invoke(block: () -> Unit): String = "(2)"

fun testLambdaReturnSelectsTypeInvoke() {
    val typeResult = Type { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>typeResult<!>

    val unitResult = Type { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

    val nothingResult = <!OVERLOAD_RESOLUTION_AMBIGUITY!>Type<!> { TODO() }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, objectDeclaration, operator, propertyDeclaration, stringLiteral */
