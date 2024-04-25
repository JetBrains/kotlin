// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Int, f: () -> Unit, y: Int) {}

fun bar() {
    var x: Int?
    x = 4
    foo(x, { x = null; x<!UNSAFE_CALL!>.<!>hashCode() }, x)
}
