// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

operator fun String.invoke(i: Int) {}

fun foo(s: String?) {
    <!UNSAFE_IMPLICIT_INVOKE_CALL!>s<!>(1)

    <!UNSAFE_IMPLICIT_INVOKE_CALL!>(s ?: null)<!>(1)
}
