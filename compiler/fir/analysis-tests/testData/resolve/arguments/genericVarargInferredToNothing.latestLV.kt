// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-78252

fun <T : Number> bounded(vararg xs: T) {}
fun <T> unbound(vararg xs: T) {}
fun <T, R> multipleTypeArguments(vararg xs: R) {}
fun <T> inferredParameter(x: T, vararg xs: T) {}

fun createNothing(): Nothing = null!!

fun test() {
    bounded<<!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_ERROR!>Nothing<!>>() // throws ClassCastException at runtime
    unbound<<!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_ERROR!>Nothing<!>>() // works fine
    multipleTypeArguments<Nothing, <!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_ERROR!>Nothing<!>>()
    inferredParameter<<!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_ERROR!>_<!>>(createNothing())
    <!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_ERROR!>inferredParameter<!>(createNothing())
}

/* GENERATED_FIR_TAGS: functionDeclaration, typeConstraint, typeParameter, vararg */
