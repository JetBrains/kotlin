// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

import kotlin.experimental.ExperimentalTypeInference

fun defaultStringLambdaOrUnitLambda(x: Int = 0, block: () -> String) = 1
fun defaultStringLambdaOrUnitLambda(block: () -> Unit) = "(2)"

fun defaultIntAndDefaultNumber(x: Int = 0, block: () -> String) = 1
fun defaultIntAndDefaultNumber(x: Number = 1.2, block: () -> Unit) = "(2)"

fun stringLambdaOrDefaultUnitLambda(block: () -> String) = 1
fun stringLambdaOrDefaultUnitLambda(x: Int = 0, block: () -> Unit) = "(2)"

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun annotatedDefaultStringLambdaOrUnitLambda(x: Int = 0, block: () -> String) = 1
fun annotatedDefaultStringLambdaOrUnitLambda(block: () -> Unit) = "(2)"

fun unitLambdasWithDefault(x: Int = 0, block: () -> Unit): Int = 1
fun unitLambdasWithDefault(block: () -> Unit): String = "(2)"

fun testDefaultStringLambdaOrUnitLambda() {
    val stringResult = defaultStringLambdaOrUnitLambda { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val parenthesizedStringResult = defaultStringLambdaOrUnitLambda({ "OK" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>parenthesizedStringResult<!>

    val explicitUnitResult = defaultStringLambdaOrUnitLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val explicitDefaultResult = defaultStringLambdaOrUnitLambda(1) { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitDefaultResult<!>

    val coercedUnitResult = defaultStringLambdaOrUnitLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>
}

fun testDefaultIntAndDefaultNumber() {
    val stringResult = defaultIntAndDefaultNumber { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = defaultIntAndDefaultNumber { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val intArgumentResult = defaultIntAndDefaultNumber(1) { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>intArgumentResult<!>

    val numberArgumentResult = defaultIntAndDefaultNumber(1.2) { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>numberArgumentResult<!>
}

fun testStringLambdaOrDefaultUnitLambda() {
    val stringResult = stringLambdaOrDefaultUnitLambda { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = stringLambdaOrDefaultUnitLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val explicitDefaultResult = stringLambdaOrDefaultUnitLambda(1) { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitDefaultResult<!>

    val coercedUnitResult = stringLambdaOrDefaultUnitLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>
}

fun testAnnotatedDefaultStringLambdaOrUnitLambda() {
    val stringResult = annotatedDefaultStringLambdaOrUnitLambda { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = annotatedDefaultStringLambdaOrUnitLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val explicitDefaultResult = annotatedDefaultStringLambdaOrUnitLambda(1) { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitDefaultResult<!>
}

fun testUnitLambdasWithDefault() {
    val stringResult = unitLambdasWithDefault { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val explicitUnitResult = unitLambdasWithDefault { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val nothingResult = unitLambdasWithDefault { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nothingResult<!>

    val coercedUnitResult = unitLambdasWithDefault { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */
