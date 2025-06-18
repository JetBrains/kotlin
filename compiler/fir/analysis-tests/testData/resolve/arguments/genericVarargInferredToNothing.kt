// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-78252

fun <T : Number> bounded(vararg xs: T) {}
fun <T> unbound(vararg xs: T) {}
fun <T, R> multipleTypeArguments(vararg xs: R) {}
fun <T> inferredParameter(x: T, vararg xs: T) {}

fun createNothing(): Nothing = null!!

fun test() {
    bounded<<!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING!>Nothing<!>>() // throws ClassCastException at runtime
    unbound<<!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING!>Nothing<!>>() // works fine
    multipleTypeArguments<Nothing, <!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING!>Nothing<!>>()
    inferredParameter<<!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING!>_<!>>(createNothing())
    <!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING!>inferredParameter<!>(createNothing())
}

/* GENERATED_FIR_TAGS: functionDeclaration, typeConstraint, typeParameter, vararg */
