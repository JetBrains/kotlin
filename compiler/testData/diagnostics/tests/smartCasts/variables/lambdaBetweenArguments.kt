// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Int, f: () -> Unit, y: Int) {}

fun bar() {
    var x: Int?
    x = 4
    foo(<!DEBUG_INFO_SMARTCAST!>x<!>, { x = null; x<!UNSAFE_CALL!>.<!>hashCode() }, <!NI;SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>x<!>)
}