fun testAssignment() {
    var <!VARIABLE_NEVER_READ!>a<!> = 1
    <!UNREACHABLE_CODE!><!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> =<!> todo()
}

class Foo {
    var property: Int = 0
}

fun testClassPropertyAssignment(foo: Foo) {
    foo.property = 1
    foo<!UNREACHABLE_CODE!>.property =<!> todo()
}

fun testVariableDeclaration() {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>a<!> =<!> todo()
}

fun testPlusAssign() {
    operator fun Int.plusAssign(i: Int) {}

    <!CAN_BE_VAL!>var<!> a = 1
    a <!UNREACHABLE_CODE!>+=<!> todo()
}

fun testClassPropertyPlusAssign(foo: Foo) {
    foo.property += 1
    foo<!UNREACHABLE_CODE!>.property<!> += todo() as Int
}


fun todo(): Nothing = throw Exception()
