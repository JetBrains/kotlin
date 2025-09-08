// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun condition(): Boolean = false

@IgnorableReturnValue fun ignCond(): Boolean = false

fun a(): Int {
    condition() || return 0
    ignCond() || throw IllegalArgumentException()

    condition() && throw IllegalArgumentException()
    ignCond() && return 1

    return -1
}

fun b() {
    condition() <!RETURN_VALUE_NOT_USED!>||<!> ignCond()
    ignCond() <!RETURN_VALUE_NOT_USED!>&&<!> condition()
    ignCond() <!RETURN_VALUE_NOT_USED!>&&<!> ignCond()
}
/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, disjunctionExpression, functionDeclaration, integerLiteral */
