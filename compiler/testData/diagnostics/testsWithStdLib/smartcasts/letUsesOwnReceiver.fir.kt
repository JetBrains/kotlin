// Based on KT-9033
fun f(s: String) = s

fun foo(s: String?) {
    s?.let { f(s) }
    s?.let { f(it) }
}