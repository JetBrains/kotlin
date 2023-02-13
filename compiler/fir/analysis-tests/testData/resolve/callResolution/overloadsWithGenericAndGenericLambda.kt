// ISSUE: KT-50646

class Inv<T>

fun <T> foo(c: Inv<T>, f: () -> T) {}
fun <T> foo(c: Inv<T>, v: T) {}

fun <T> test(x: Inv<T>, v: T) {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(x) { v }
}
