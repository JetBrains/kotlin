// ISSUE: KT-65241

object A

fun test() {
    A.<!SYNTAX!>else<!> = <!ASSIGNMENT_TYPE_MISMATCH!>42<!>
}
