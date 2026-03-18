// RUN_PIPELINE_TILL: FRONTEND
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

    unitConversion(<!ARGUMENT_TYPE_MISMATCH!>returnStringLambda<!>)
    unitConversion(<!ARGUMENT_TYPE_MISMATCH!>returnStringRef<!>)
    unitConversion(<!ARGUMENT_TYPE_MISMATCH!>returnNullLambda<!>)
    unitConversion(<!ARGUMENT_TYPE_MISMATCH!>returnNullRef<!>)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, lambdaLiteral, nullableType,
propertyDeclaration */
