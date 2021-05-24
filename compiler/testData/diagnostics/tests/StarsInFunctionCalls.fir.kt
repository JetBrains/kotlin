
fun <T> getT() {}
fun <A, B> getTT() {}
fun <A, B, C> getTTT(x : Any) {}
fun foo(a : Any?) {}

public fun main() {
    getT<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
    <!UNRESOLVED_REFERENCE!>ggetT<!><<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
    getTT<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>, <!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
    getTT<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>, Int>()
    getTT<Int, <!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
    foo(getTTT<Int, <!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>, Int>(1))
}
