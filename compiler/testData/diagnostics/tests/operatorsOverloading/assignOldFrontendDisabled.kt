// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class C

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun C.assign(a: String) {
}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Int.assign(a: String) {
}

/**
 * Test that diagnostics work if assign operator overload feature is disabled
 */
fun test() {
    val c = C()
    <!VAL_REASSIGNMENT!>c<!> = <!TYPE_MISMATCH!>"hello"<!>
    c = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>

    val integer = 5
    <!VAL_REASSIGNMENT!>integer<!> = <!TYPE_MISMATCH!>"Five"<!>
}
