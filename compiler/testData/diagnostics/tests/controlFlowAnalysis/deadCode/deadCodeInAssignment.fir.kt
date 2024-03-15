fun testAssignment() {
    var <!VARIABLE_NEVER_READ!>a<!> = 1
    <!UNREACHABLE_CODE!><!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> =<!> todo()
}

fun testVariableDeclaration() {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>a<!> =<!> todo()
}

fun testPlusAssign() {
    operator fun Int.plusAssign(i: Int) {}

    <!CAN_BE_VAL!>var<!> a = 1
    a <!UNREACHABLE_CODE!>+=<!> todo()
}


fun todo(): Nothing = throw Exception()
