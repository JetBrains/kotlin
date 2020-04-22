// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo1(f: suspend () -> Unit) {}
fun bar1() {}

fun <T> foo2(e: T, f: suspend (T) -> Unit) {}
fun bar2(x: Int) {}
fun bar2(s: String) {}

fun test() {
    <!INAPPLICABLE_CANDIDATE!>foo1<!>(::bar1)

    <!INAPPLICABLE_CANDIDATE!>foo2<!>(42, <!UNRESOLVED_REFERENCE!>::bar2<!>)
    <!INAPPLICABLE_CANDIDATE!>foo2<!>("str", <!UNRESOLVED_REFERENCE!>::bar2<!>)

    <!INAPPLICABLE_CANDIDATE!>foo2<!>(42, ::bar1)
}
