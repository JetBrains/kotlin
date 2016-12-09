// !LANGUAGE: -OperatorRem
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

class Foo {
    <!UNSUPPORTED_FEATURE!>operator<!> fun rem(x: Int): Foo = Foo()
}

class Bar {
    <!UNSUPPORTED_FEATURE!>operator<!> fun remAssign(x: Int) {}
}

class Baz {
    companion object {
        <!UNSUPPORTED_FEATURE!>operator<!> fun rem(x: Int) {}
        <!UNSUPPORTED_FEATURE!>operator<!> fun Int.rem(x: Int) {}
    }
}

<!UNSUPPORTED_FEATURE!>operator<!> fun Baz.rem(x: Int) {}

fun local() {
    <!UNSUPPORTED_FEATURE!>operator<!> fun Int.rem(x: Int) {}
    <!UNSUPPORTED_FEATURE!>operator<!> fun String.remAssign(x: Int) {}
}