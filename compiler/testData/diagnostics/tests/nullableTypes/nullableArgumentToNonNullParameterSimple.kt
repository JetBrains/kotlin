// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: String) {}
fun foo(x: Int) {}
fun foo(x: Int, y: String) {}

fun bar(nullX: Int?, nullY: String?, notNullY: String) {
    foo(<!TYPE_MISMATCH!>nullX<!>)
    foo(<!TYPE_MISMATCH!>nullX<!>, notNullY)
    foo(<!TYPE_MISMATCH!>nullX<!>, <!TYPE_MISMATCH!>nullY<!>)
    <!NONE_APPLICABLE!>foo<!>()
}