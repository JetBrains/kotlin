// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun samAndFun(block: () -> Unit): Int = 1

fun interface samAndFun {
    fun run(): String
}

fun samAndFunAndDefault(block: () -> String, value: Int = 1): Int = 1

fun interface samAndFunAndDefault {
    fun run()
}

fun testSamAndFun() {
    val constructorResult = samAndFun { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("samAndFun")!>constructorResult<!>

    val unitFunctionResult = samAndFun { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>unitFunctionResult<!>

    val nothingFunctionResult = samAndFun { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nothingFunctionResult<!>

    val coercedFunctionResult = samAndFun { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>coercedFunctionResult<!>

    val parenthesizedConstructorResult = samAndFun({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("samAndFun")!>parenthesizedConstructorResult<!>
}

fun testSamAndFunAndDefault() {
    val parenthesizedFunctionResult = samAndFunAndDefault({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>parenthesizedFunctionResult<!>

    val constructorResult = samAndFunAndDefault { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("samAndFunAndDefault")!>constructorResult<!>
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
lambdaLiteral, localProperty, propertyDeclaration, stringLiteral */
