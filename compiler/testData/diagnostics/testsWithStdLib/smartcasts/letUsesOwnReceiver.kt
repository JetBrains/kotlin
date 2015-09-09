// Based on KT-9033
fun f(s: String) = s

fun foo(s: String?) {
    s?.let { f(<!DEBUG_INFO_SMARTCAST!>s<!>) }
    s?.let { f(it) }
}