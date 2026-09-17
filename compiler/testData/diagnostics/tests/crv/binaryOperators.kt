// RUN_PIPELINE_TILL: BACKEND

@file:MustUseReturnValues

fun arithmetics(a: Int, b: Int) {
    a <!RETURN_VALUE_NOT_USED!>+<!> b
    a <!RETURN_VALUE_NOT_USED!>-<!> b
    a <!RETURN_VALUE_NOT_USED!>*<!> b
    a <!RETURN_VALUE_NOT_USED!>/<!> b
    a <!RETURN_VALUE_NOT_USED!>%<!> b
}

fun booleanOperators(a: Boolean, b: Boolean) {
    a <!RETURN_VALUE_NOT_USED!>||<!> b
    a <!RETURN_VALUE_NOT_USED!>&&<!> b
}

fun <T: Comparable<T>> comparison(a: T, b: T) {
    a <!RETURN_VALUE_NOT_USED!>==<!> b
    a <!RETURN_VALUE_NOT_USED!>!=<!> b
    a <!RETURN_VALUE_NOT_USED!>===<!> b
    a <!RETURN_VALUE_NOT_USED!>!==<!> b
    a <!RETURN_VALUE_NOT_USED!><<!> b
    a <!RETURN_VALUE_NOT_USED!><=<!> b
    a <!RETURN_VALUE_NOT_USED!>><!> b
    a <!RETURN_VALUE_NOT_USED!>>=<!> b
}
