// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test(a: Int, b: Boolean) {
    bar(a.foo(<!ARGUMENT_TYPE_MISMATCH!>b<!>))
}

fun <T, R> T.foo(l: (T) -> R): R = TODO()

fun <S> bar(a: S) {}
