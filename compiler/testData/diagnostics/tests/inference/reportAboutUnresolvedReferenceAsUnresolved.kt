fun <T, U> T.map(f: (T) -> U) = f(this)

fun consume(<!UNUSED_PARAMETER!>s<!>: String) {}

fun test() {
    <!UNREACHABLE_CODE!>consume(<!>1.map(::<!UNRESOLVED_REFERENCE!>foo<!>)<!UNREACHABLE_CODE!>)<!>
}