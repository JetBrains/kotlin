// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun String.invoke(i: Int) {}

fun foo(i: Int) {
    <!INAPPLICABLE_CANDIDATE!>i<!>(1)

    <!INAPPLICABLE_CANDIDATE!>1(1)<!>
}