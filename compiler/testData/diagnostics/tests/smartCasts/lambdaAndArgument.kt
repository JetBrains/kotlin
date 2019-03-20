// !DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun <T> foo(t1: T, t2: T) = t1 ?: t2

inline fun <T> bar(<!UNUSED_PARAMETER!>l<!>: (T) -> Unit): T = null!!

fun use() {
    var x: Int?
    x = 5
    // Write is AFTER
    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
    // x is nullable at the second argument
    <!UNREACHABLE_CODE!>foo(<!><!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>bar<!> { x = null }, <!UNREACHABLE_CODE!>x!!)<!>
}
