// IGNORE_FIR_DIAGNOSTICS
// IGNORE_ERRORS
// FIR_IDENTICAL

class C {
    companion <!REDECLARATION!>object<!> {}

    val <!REDECLARATION!>Companion<!> = C
}
