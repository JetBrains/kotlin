fun f(a: Boolean, b: Int) {}

fun foo(a: Any) {
    <!INAPPLICABLE_CANDIDATE!>f<!>(a is Int, a)
    1 <!NONE_APPLICABLE!>+<!> a
}
