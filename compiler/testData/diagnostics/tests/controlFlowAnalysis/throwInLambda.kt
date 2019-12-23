// See KT-10913 Bogus unreachable code warning

fun fn() : String? = null
fun foo(): String {
    val x = fn()?.let { throw Exception() } ?: "unreachable?"
    return x
}
fun bar(): String {
    val x = fn() ?: return ""
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>y<!> =<!> x<!UNNECESSARY_SAFE_CALL!>?.<!>let { throw Exception() } <!UNREACHABLE_CODE, USELESS_ELVIS!>?: "unreachable"<!>
    <!UNREACHABLE_CODE!>return y<!>
}