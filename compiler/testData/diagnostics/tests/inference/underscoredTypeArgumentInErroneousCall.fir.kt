// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-69739

import <!UNRESOLVED_IMPORT!>some<!>.convTo

fun <T> foo() {}

fun main() {
    <!UNRESOLVED_REFERENCE!>convTo<!><<!OTHER_ERROR!>_<!>>()
    <!UNRESOLVED_REFERENCE!>convTo<!><<!OTHER_ERROR!>_<!>>

    foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, <!OTHER_ERROR!>_<!>><!>()
}
