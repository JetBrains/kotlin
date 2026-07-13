// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

class SecondaryConstructor {
    constructor(block: () -> Unit)
}

fun SecondaryConstructor(block: () -> String): String = "function"

class PrimaryConstructor(val block: () -> String)

fun PrimaryConstructor(block: () -> Unit): String = "function"

class PrimaryWithDefaultLambda(val block: () -> String = { "" })

fun PrimaryWithDefaultLambda(block: () -> Unit): String = "function"

class PrimaryWithDefault(val value: Int = 9, val block: () -> String)

fun PrimaryWithDefault(block: () -> Unit): String = "function"

fun testSecondaryConstructorAndFunction() {
    val functionResult = SecondaryConstructor { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>functionResult<!>

    val parenthesizedFunctionResult = SecondaryConstructor({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>parenthesizedFunctionResult<!>

    val constructorResult = SecondaryConstructor { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("SecondaryConstructor")!>constructorResult<!>

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>SecondaryConstructor<!> { TODO() }
}

fun testPrimaryConstructorAndFunction() {
    val constructorResult = PrimaryConstructor { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("PrimaryConstructor")!>constructorResult<!>

    val parenthesizedConstructorResult = PrimaryConstructor({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("PrimaryConstructor")!>parenthesizedConstructorResult<!>

    val functionResult = PrimaryConstructor { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>functionResult<!>

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>PrimaryConstructor<!> { TODO() }
}

fun testPrimaryWithDefaultAndFunction() {
    val constructorResult = PrimaryWithDefaultLambda { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("PrimaryWithDefaultLambda")!>constructorResult<!>

    val explicitUnitResult = PrimaryWithDefaultLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>PrimaryWithDefaultLambda<!> { TODO() }

    val coercedUnitResult = PrimaryWithDefaultLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>
}

fun testPrimaryWithDefaultBeforeLambdaAndFunction() {
    val constructorResult = PrimaryWithDefault { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("PrimaryWithDefault")!>constructorResult<!>

    val parenthesizedStringResult = PrimaryWithDefault({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>parenthesizedStringResult<!>

    val parenthesizedUnitResult = PrimaryWithDefault({ Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>parenthesizedUnitResult<!>

    val parenthesizedNothingResult = PrimaryWithDefault({ TODO() })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>parenthesizedNothingResult<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, primaryConstructor, propertyDeclaration, secondaryConstructor, stringLiteral */
