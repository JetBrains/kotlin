// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(c: C): Int? = null
}

fun test(c: C) {
    c <!COMPARE_TO_TYPE_MISMATCH!><<!> c
    c <!COMPARE_TO_TYPE_MISMATCH!><=<!> c
    c <!COMPARE_TO_TYPE_MISMATCH!>>=<!> c
    c <!COMPARE_TO_TYPE_MISMATCH!>><!> c
}

/* GENERATED_FIR_TAGS: classDeclaration, comparisonExpression, functionDeclaration, nullableType, operator */
