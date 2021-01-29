interface A
interface B

fun fooB(b: B) {}

fun <T> bar(f: (T) -> Unit, e: T) {}
fun <T> baz(e: T, f: (T) -> Unit) {}

fun test(a: A, b: B) {
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>baz<!>(a, <!UNRESOLVED_REFERENCE!>::fooB<!>)<!>
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>bar<!>(<!UNRESOLVED_REFERENCE!>::fooB<!>, a)<!>
}
