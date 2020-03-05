// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Int, f: () -> Unit, y: Int) {}

fun bar() {
    var x: Int?
    x = 4
    foo(x, { x = null; x.<!INAPPLICABLE_CANDIDATE!>hashCode<!>() }, x)
}