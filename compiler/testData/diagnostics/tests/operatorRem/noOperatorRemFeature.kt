// !LANGUAGE: -OperatorRem
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE, -EXTENSION_SHADOWED_BY_MEMBER

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

fun builtIns(b: Byte, s: Short) {
    var a = 1 <!UNRESOLVED_REFERENCE!>%<!> 2
    var d = 5
    d <!UNRESOLVED_REFERENCE!>%=<!> 3
    b <!UNRESOLVED_REFERENCE!>%<!> s
    1.0 <!UNRESOLVED_REFERENCE!>%<!> 2.0
}