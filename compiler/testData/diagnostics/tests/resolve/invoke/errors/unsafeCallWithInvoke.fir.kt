// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

operator fun String.invoke(i: Int) {}

fun foo(s: String?) {
    <!INAPPLICABLE_CANDIDATE!>s<!>(1)

    <!INAPPLICABLE_CANDIDATE!>(s ?: null)(1)<!>
}
