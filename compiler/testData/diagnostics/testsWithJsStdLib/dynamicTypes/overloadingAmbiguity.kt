// FIR_IDENTICAL
// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <!DYNAMIC_RECEIVER_NOT_ALLOWED!>dynamic<!>.foo(s: String, a: Any) {}
fun <!DYNAMIC_RECEIVER_NOT_ALLOWED!>dynamic<!>.foo(s: Any, a: String) {}

fun test(d: dynamic) {
    d.<!DEBUG_INFO_DYNAMIC!>foo<!>(1, "")
    d.<!DEBUG_INFO_DYNAMIC!>foo<!>("", "")
    d.<!DEBUG_INFO_DYNAMIC!>foo<!>(1, 1)
}