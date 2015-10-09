fun testAssignment() {
    var <!UNUSED_VARIABLE!>a<!> = 1
    <!UNREACHABLE_CODE!>a =<!> todo()
}

fun testVariableDeclaration() {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>a<!> =<!> todo()
}

fun testPlusAssign() {
    operator fun Int.plusAssign(<!UNUSED_PARAMETER!>i<!>: Int) {}

    var a = 1
    a <!UNREACHABLE_CODE!>+=<!> todo()
}


fun todo(): Nothing = throw Exception()