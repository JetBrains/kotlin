// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun dynamic.foo(s: String, a: Any) {}
fun dynamic.foo(s: Any, a: String) {}

fun test(d: dynamic) {
    d.foo(1, "")
    d.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>("", "")
    d.<!DEBUG_INFO_DYNAMIC!>foo<!>(1, 1)
}