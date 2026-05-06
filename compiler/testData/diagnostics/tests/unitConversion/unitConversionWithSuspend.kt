// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// ISSUE: KT-84393
// DIAGNOSTICS: -UNUSED_PARAMETER

fun suspendUnitConversion(param: suspend () -> Unit) {}

fun returnString(): String = ""

val stringLambda: () -> String = { "" }

fun test() {
    suspendUnitConversion(::returnString)
    suspendUnitConversion(stringLambda)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, lambdaLiteral, propertyDeclaration,
stringLiteral, suspend */
