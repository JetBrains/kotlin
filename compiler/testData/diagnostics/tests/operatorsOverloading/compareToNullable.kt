// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(c: C): Int? = null
}

fun test(c: C) {
    c <!COMPARE_TO_TYPE_MISMATCH!><<!> c
    c <!COMPARE_TO_TYPE_MISMATCH!><=<!> c
    c <!COMPARE_TO_TYPE_MISMATCH!>>=<!> c
    c <!COMPARE_TO_TYPE_MISMATCH!>><!> c
}