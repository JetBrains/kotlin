// ISSUE: KT-69739

import <!UNRESOLVED_IMPORT!>some<!>.convTo

fun <T> foo() {}

fun main() {
    <!UNRESOLVED_REFERENCE!>convTo<!><<!OTHER_ERROR!>_<!>>()
    <!UNRESOLVED_REFERENCE!>convTo<!><<!OTHER_ERROR!>_<!>>

    <!INAPPLICABLE_CANDIDATE!>foo<!><String, <!OTHER_ERROR!>_<!>>()
}
