interface A
interface B

fun fooB(b: B) {}

fun <T> bar(f: (T) -> Unit, e: T) {}
fun <T> baz(e: T, f: (T) -> Unit) {}

fun test(a: A, b: B) {
    <!INAPPLICABLE_CANDIDATE!>baz<!>(a, <!UNRESOLVED_REFERENCE!>::fooB<!>)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(<!UNRESOLVED_REFERENCE!>::fooB<!>, a)
}
