// !DIAGNOSTICS: -NOTHING_TO_INLINE
// NI_EXPECTED_FILE
// See KT-9143: smart cast on a variable nulled inside a lambda argument
inline fun <T> foo(t1: T, t2: T) = t1 ?: t2

inline fun <T> bar(<!UNUSED_PARAMETER!>l<!>: (T) -> Unit): T = null!!

fun use() {
    var x: Int?
    x = 5
    // Write to x is AFTER
    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
    // No smart cast should be here!
    foo(bar { x = null }, <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode())
}
