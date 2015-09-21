// !DIAGNOSTICS: -UNUSED_PARAMETER

operator fun String.invoke(i: Int) {}

fun foo(s: String?) {
    <!UNSAFE_CALL!>s<!>(1)

    <!UNSAFE_CALL!>(s ?: null)<!>(1)
}
