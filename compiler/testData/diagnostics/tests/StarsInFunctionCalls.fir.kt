// !WITH_NEW_INFERENCE

fun <T> getT() {}
fun <A, B> getTT() {}
fun <A, B, C> getTTT(x : Any) {}
fun foo(a : Any?) {}

public fun main() {
    getT<*>()
    <!UNRESOLVED_REFERENCE!>ggetT<!><*>()
    getTT<*, *>()
    getTT<*, Int>()
    getTT<Int, *>()
    foo(getTTT<Int, *, Int>(1))
}
