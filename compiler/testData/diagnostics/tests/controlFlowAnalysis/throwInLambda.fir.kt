// !LANGUAGE: -SafeCallsAreAlwaysNullable
// See KT-10913 Bogus unreachable code warning

fun fn() : String? = null
fun foo(): String {
    val x = fn()?.let { throw Exception() } ?: "unreachable?"
    return x
}
fun bar(): String {
    val x = fn() ?: return ""
    val y = <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>x<!UNNECESSARY_SAFE_CALL!>?.<!>let { throw Exception() }<!> <!USELESS_ELVIS!>?: "unreachable"<!>
    return y
}
