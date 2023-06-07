/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 2 -> sentence 1
 * expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 */

// !DIAGNOSTICS: -UNUSED_PARAMETER

infix fun Any.sealed(a: Any?) {}

val x = 1 <!UNSUPPORTED_SEALED_WHEN!>sealed<!> when (1) {
    1 -> 1
    else -> 2
}

val x1 = 1 <!UNSUPPORTED_SEALED_WHEN!>sealed<!> /**/ when (1) {
    1 -> 1
    else -> 2
}

fun foo() {
    <!UNRESOLVED_REFERENCE, UNSUPPORTED_SEALED_WHEN!>sealed<!><!SYNTAX!><!> when {
        else -> {}
    }

    1 <!UNSUPPORTED_SEALED_WHEN!>sealed<!> when {
        else -> {}
    }

    1 sealed (when {
        else -> {}
    })

    1
    <!UNRESOLVED_REFERENCE, UNSUPPORTED_SEALED_WHEN!>sealed<!><!SYNTAX!><!> when {
        else -> {}
    }

    1 <!UNSUPPORTED_SEALED_WHEN!>sealed<!>
    when {
        else -> {}
    }
}
