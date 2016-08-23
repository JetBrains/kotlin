// !DIAGNOSTICS: -UNUSED_PARAMETER

operator fun String.invoke(i: Int) {}

fun foo(s: String?) {
    <!UNSAFE_CALL!>s<!>(1)

    <!UNSAFE_CALL!>(s <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)<!>(1)
}
