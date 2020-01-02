// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: String) {}
fun foo(x: Int) {}
fun foo(x: Int, y: String) {}

fun bar(nullX: Int?, nullY: String?, notNullY: String) {
    foo(nullX)
    foo(nullX, notNullY)
    foo(nullX, nullY)
    <!INAPPLICABLE_CANDIDATE!>foo<!>()
}