// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun <T> takeFun(f: (T) -> Unit) {}
fun <T, R> callFun(f: (T) -> R): R = TODO()

fun <T> foo(s: T) {}
fun <T : Int> fooInt(s: T) {}

open class Wrapper<T>(val value: T)
fun <T, R : Wrapper<in T>> createWrapper(s: T): R = TODO()

fun <T> Wrapper<T>.baz(transform: (T) -> Unit): T = TODO()

fun test() {
    takeFun<String>(::foo)
    <!INAPPLICABLE_CANDIDATE!>takeFun<!><String>(<!UNRESOLVED_REFERENCE!>::fooInt<!>)

    callFun<String, Wrapper<String>>(::createWrapper)
    callFun<Int, Wrapper<Number>>(::createWrapper)
    callFun<String, Wrapper<*>>(::createWrapper)
    callFun<String, Wrapper<Int>>(::createWrapper)

    callFun<Int, Wrapper<Int>>(::createWrapper).baz(::foo)
}
