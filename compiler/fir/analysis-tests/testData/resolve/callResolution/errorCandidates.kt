fun foo(x: Int) {}

interface A
interface B
interface C : A, B

fun bar(x: A) {}
fun bar(x: B) {}

fun test(c: C) {
    // Argument mapping error
    <!INAPPLICABLE_CANDIDATE!>foo<!>("")

    // Ambiguity
    <!AMBIGUITY!>bar<!>(c)

    // Unresolved reference
    <!UNRESOLVED_REFERENCE!>baz<!>()
}