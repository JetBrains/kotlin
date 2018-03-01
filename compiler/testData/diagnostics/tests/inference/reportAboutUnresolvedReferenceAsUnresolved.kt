fun <T, U> T.map(f: (T) -> U) = f(this)

fun consume(<!UNUSED_PARAMETER!>s<!>: String) {}

fun test() {
    consume(1.map(::<!UNRESOLVED_REFERENCE!>foo<!>))
}