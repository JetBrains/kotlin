// FIR_IDENTICAL
fun <T, U> T.map(f: (T) -> U) = f(this)

fun consume(s: String) {}

fun test() {
    consume(1.map(::<!UNRESOLVED_REFERENCE!>foo<!>))
}
