// !DIAGNOSTICS: -UNUSED_PARAMETER

infix fun Any.sealed(a: Any?) {}

val x = 1 sealed when (1) {
    1 -> 1
    else -> 2
}

val x1 = 1 sealed /**/ when (1) {
    1 -> 1
    else -> 2
}

fun foo() {
    <!UNRESOLVED_REFERENCE!>sealed<!><!SYNTAX!><!> when {
        else -> {}
    }

    1 sealed when {
        else -> {}
    }

    1 sealed (when {
        else -> {}
    })

    1
    <!UNRESOLVED_REFERENCE!>sealed<!><!SYNTAX!><!> when {
        else -> {}
    }

    1 sealed
    when {
        else -> {}
    }
}