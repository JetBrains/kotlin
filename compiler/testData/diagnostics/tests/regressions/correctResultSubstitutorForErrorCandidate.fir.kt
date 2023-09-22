// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test(a: Int, b: Boolean) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>(a.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(<!ARGUMENT_TYPE_MISMATCH!>b<!>))
}

fun <T, R> T.foo(l: (T) -> R): R = TODO()

fun <S> bar(a: S) {}
