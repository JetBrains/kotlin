interface A
interface B

fun fooB(b: B) {}

fun <T> bar(f: (T) -> Unit, e: T) {}
fun <T> baz(e: T, f: (T) -> Unit) {}

fun test(a: A, b: B) {
    <!INAPPLICABLE_CANDIDATE!>baz<!>(a, ::fooB)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(::fooB, a)
}
