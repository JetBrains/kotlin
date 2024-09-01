// ISSUE: KT-69739

import <!UNRESOLVED_REFERENCE!>some<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>convTo<!>

fun <T> foo() {}

fun main() {
    <!UNRESOLVED_REFERENCE!>convTo<!><_>()
    <!UNRESOLVED_REFERENCE!>convTo<!><_>

    foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, _><!>()
}
