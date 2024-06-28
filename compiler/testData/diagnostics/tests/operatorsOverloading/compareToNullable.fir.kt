// DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(c: C): Int? = null
}

fun test(c: C) {
    c < c
    c <= c
    c >= c
    c > c
}
