// !LANGUAGE: -OperatorRem
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

class Foo {
    <!UNSUPPORTED_FEATURE!>operator<!> fun rem(x: Int): Foo = Foo()
}

class Bar {
    <!UNSUPPORTED_FEATURE!>operator<!> fun remAssign(x: Int) {}
}

fun baz() {
    val f = Foo() % 1
    val b = Bar()
    b %= 1
}