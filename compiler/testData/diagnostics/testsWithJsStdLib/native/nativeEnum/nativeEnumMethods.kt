external enum class Foo {
    A,
    B
}

fun manipulateWithEnum(x: Enum<*>): Int {
    return x.ordinal
}

fun main() {
    Foo.values()
    Foo.valueOf("A")

    // enumValues<Foo>()
    // enumValueOf<Foo>("A")

    Foo.A.<!UNRESOLVED_REFERENCE!>name<!>
    Foo.B.<!UNRESOLVED_REFERENCE!>ordinal<!>
    Foo.A.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>compareTo<!>(Foo.B)

    manipulateWithEnum(<!TYPE_MISMATCH!>Foo.A<!>)
}