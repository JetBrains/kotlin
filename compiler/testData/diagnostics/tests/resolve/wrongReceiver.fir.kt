package some

class A()

val Int.some: Int get() = 4
val Int.foo: Int get() = 4

fun Int.extFun() = 4

fun String.test() {
    <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>some<!>
    some.A()
    "".<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>some<!>

    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
    "".<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>

    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extFun<!>()
    "".<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extFun<!>()
}
