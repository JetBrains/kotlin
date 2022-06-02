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

    enumValues<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Foo<!>>()
    enumValueOf<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Foo<!>>("A")

    // TODO: After a bootstraping those two lines should be uncommented and lines above should be deleted
    // BECAUSE kotlin.js STANDARD LIBRARY WAS EXTENDED WITH OVERLOADS OF `enumValues` AND `enumValueOf` METHODS

    // enumValues<Foo>()
    // enumValueOf<Foo>("A")

    Foo.A.<!UNRESOLVED_REFERENCE!>name<!>
    Foo.B.<!UNRESOLVED_REFERENCE!>ordinal<!>
    Foo.A.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>compareTo<!>(Foo.B)

    manipulateWithEnum(<!TYPE_MISMATCH!>Foo.A<!>)
}