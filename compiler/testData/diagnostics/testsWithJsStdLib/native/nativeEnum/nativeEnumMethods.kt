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

    // AFTER A BOOTSTRAP THOSE TWO LINES SHOULD BE UNCOMMENTED AND LINES ABOVE SHOULD BE DELETED
    // BECAUSE kotlin.js STANDARD LIBRARY WAS EXTENDED WITH OVERLOADS OF `enumValues` AND `enumValueOf` METHODS

    // enumValues<Foo>()
    // enumValueOf<Foo>("A")

    Foo.A.<!UNRESOLVED_REFERENCE!>name<!>
    Foo.B.<!UNRESOLVED_REFERENCE!>ordinal<!>
    Foo.A.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>compareTo<!>(Foo.B)

    manipulateWithEnum(<!TYPE_MISMATCH!>Foo.A<!>)
}