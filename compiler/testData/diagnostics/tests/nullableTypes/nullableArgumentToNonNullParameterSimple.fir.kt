// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: String) {}
fun foo(x: Int) {}
fun foo(x: Int, y: String) {}

fun bar(nullX: Int?, nullY: String?, notNullY: String) {
    <!NONE_APPLICABLE!>foo<!>(nullX)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(nullX, notNullY)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(nullX, nullY)
    <!NONE_APPLICABLE!>foo<!>()
}
