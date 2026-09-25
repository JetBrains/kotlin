// RUN_PIPELINE_TILL: CODEGEN

@file:MustUseReturnValues

fun arithmetics(a: Int, b: Int) {
    a <!RETURN_VALUE_NOT_USED!>+<!> b
    a <!RETURN_VALUE_NOT_USED!>-<!> b
    a <!RETURN_VALUE_NOT_USED!>*<!> b
    a <!RETURN_VALUE_NOT_USED!>/<!> b
    a <!RETURN_VALUE_NOT_USED!>%<!> b
    <!RETURN_VALUE_NOT_USED!>-<!>a
}

fun booleanOperators(a: Boolean, b: Boolean) {
    a <!RETURN_VALUE_NOT_USED!>||<!> b
    a <!RETURN_VALUE_NOT_USED!>&&<!> b
    <!RETURN_VALUE_NOT_USED!>!<!>a
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

fun typeOperators(a: Any) {
    a <!RETURN_VALUE_NOT_USED!>as?<!> Int
    a as Number // result in smartcast, thus always "used"
    a <!RETURN_VALUE_NOT_USED!>is<!> Int
    a <!RETURN_VALUE_NOT_USED!>!is<!> Long
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, annotationUseSiteTargetFile, comparisonExpression,
disjunctionExpression, equalityExpression, functionDeclaration, multiplicativeExpression, typeConstraint, typeParameter */
