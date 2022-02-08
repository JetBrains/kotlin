// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test(a: Int, b: Boolean) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>(a.foo(<!ARGUMENT_TYPE_MISMATCH!>b<!>))
}

fun <T, R> T.foo(l: (T) -> R): R = TODO()

fun <S> bar(a: S) {}
