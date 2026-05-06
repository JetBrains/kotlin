// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// ISSUE: KT-84393
// DIAGNOSTICS: -UNUSED_PARAMETER

fun unitConversion(param: () -> Unit) {}

fun returnString(): String = ""
fun returnNull() = null

val returnStringLambda: () -> String = { -> "" }
val returnNullLambda = { -> null }

val returnStringRef = ::returnString
val returnNullRef = ::returnNull

fun main() {
    unitConversion { "" }
    unitConversion(::returnString)
    unitConversion(::returnNull)

    unitConversion(returnStringLambda)
    unitConversion(returnStringRef)
    unitConversion(returnNullLambda)
    unitConversion(returnNullRef)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, lambdaLiteral, nullableType,
propertyDeclaration */
