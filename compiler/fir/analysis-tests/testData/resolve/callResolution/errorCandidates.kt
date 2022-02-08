fun foo(x: Int) {}

interface A
interface B
interface C : A, B

fun bar(x: A) {}
fun bar(x: B) {}

fun test(c: C) {
    // Argument mapping error
    foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)

    // Ambiguity
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>(c)

    // Unresolved reference
    <!UNRESOLVED_REFERENCE!>baz<!>()
}
