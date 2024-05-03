fun testAssignment() {
    var <!UNUSED_VARIABLE!>a<!> = 1
    <!UNREACHABLE_CODE!>a =<!> todo()
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
    operator fun Int.plusAssign(<!UNUSED_PARAMETER!>i<!>: Int) {}

    var a = 1
    a <!UNREACHABLE_CODE!>+=<!> todo()
}

fun testClassPropertyPlusAssign(foo: Foo) {
    foo.property += 1
    foo.property <!UNREACHABLE_CODE!>+=<!> todo() as Int
}


fun todo(): Nothing = throw Exception()