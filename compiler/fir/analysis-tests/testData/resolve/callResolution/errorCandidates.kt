fun foo(x: Int) {}

interface A
interface B
interface C : A, B

fun bar(x: A) {}
fun bar(x: B) {}

fun test(c: C) {
    // Argument mapping error
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>foo<!>("")<!>

    // Ambiguity
    <!AMBIGUITY{LT}!><!AMBIGUITY{PSI}!>bar<!>(c)<!>

    // Unresolved reference
    <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>baz<!>()<!>
}
