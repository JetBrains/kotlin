fun getT<T>() {}
fun getTT<A, B>() {}
fun getTTT<A, B, C>(<!UNUSED_PARAMETER!>x<!> : Any) {}
fun foo(<!UNUSED_PARAMETER!>a<!> : Any?) {}

public fun main() {
    getT<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
    <!UNRESOLVED_REFERENCE!>ggetT<!><<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
    getTT<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>, <!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
    getTT<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>, Int>()
    getTT<Int, <!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
    foo(getTTT<Int, <!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>, Int>(1))
}
