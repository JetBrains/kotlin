// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test(a: Int, b: Boolean) {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(a.<!INAPPLICABLE_CANDIDATE!>foo<!>(b))
}

fun <T, R> T.foo(l: (T) -> R): R = TODO()

fun <S> bar(a: S) {}
