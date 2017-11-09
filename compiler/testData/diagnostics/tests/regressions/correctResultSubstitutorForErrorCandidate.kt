// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test(a: Int, b: Boolean) {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>(a.<!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>foo<!>(<!TYPE_MISMATCH!>b<!>))
}

fun <T, R> T.foo(l: (T) -> R): R = TODO()

fun <S> bar(a: S) {}
