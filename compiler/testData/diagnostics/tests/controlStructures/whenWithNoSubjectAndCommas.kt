// FIR_IDENTICAL
fun someFun() {
    when {
        <!EXPECTED_CONDITION!>is <!UNRESOLVED_REFERENCE!>SomeClass<!><!><!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> <!EXPECTED_CONDITION!>is <!UNRESOLVED_REFERENCE!>OtherClass<!><!><!SYNTAX!><!>
    }

    val x = 0
    when {
        x == 1<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> x == 2 -> {}
    }
}
