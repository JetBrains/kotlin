// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo1(f: suspend () -> Unit) {}
fun bar1() {}

fun <T> foo2(e: T, f: suspend (T) -> Unit) {}
fun bar2(x: Int) {}
fun bar2(s: String) {}

fun test() {
    foo1(::bar1)

    foo2(42, ::bar2)
    foo2("str", ::bar2)

    foo2(42, ::<!INAPPLICABLE_CANDIDATE!>bar1<!>)
}
