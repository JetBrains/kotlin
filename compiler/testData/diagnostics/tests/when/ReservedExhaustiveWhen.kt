/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 2 -> sentence 1
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 */

// !DIAGNOSTICS: -UNUSED_PARAMETER

infix fun Any.sealed(a: Any?) {}

val x = 1 <!UNSUPPORTED!>sealed<!> when (1) {
    1 -> 1
    else -> 2
}

val x1 = 1 <!UNSUPPORTED!>sealed<!> /**/ when (1) {
    1 -> 1
    else -> 2
}

fun foo() {
    <!UNRESOLVED_REFERENCE, UNSUPPORTED!>sealed<!><!SYNTAX!><!> when {
        else -> {}
    }

    1 <!UNSUPPORTED!>sealed<!> when {
        else -> {}
    }

    1 sealed (when {
        else -> {}
    })

    <!UNUSED_EXPRESSION!>1<!>
    <!UNRESOLVED_REFERENCE, UNSUPPORTED!>sealed<!><!SYNTAX!><!> when {
        else -> {}
    }

    1 <!UNSUPPORTED!>sealed<!>
    when {
        else -> {}
    }
}