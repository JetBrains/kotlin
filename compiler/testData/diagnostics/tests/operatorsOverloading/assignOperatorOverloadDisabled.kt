// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun A.assign(a: String) {
}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Int.assign(a: String) {
}

/**
 * Test that diagnostics work if assign operator overload feature is disabled
 */
fun test() {
    val a = A()
    <!VAL_REASSIGNMENT!>a<!> = <!TYPE_MISMATCH!>"hello"<!>
    a = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>

    val integer = 5
    <!VAL_REASSIGNMENT!>integer<!> = <!TYPE_MISMATCH!>"Five"<!>
}
