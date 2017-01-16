// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun <T> takeFun(f: (T) -> Unit) {}
fun <T, R> callFun(f: (T) -> R): R = TODO()

fun <T> foo(s: T) {}
fun <T : <!FINAL_UPPER_BOUND!>Int<!>> fooInt(s: T) {}

open class Wrapper<T>(val value: T)
fun <T, R : Wrapper<in T>> createWrapper(s: T): R = TODO()

fun <T> Wrapper<T>.baz(transform: (T) -> Unit): T = TODO()

fun test() {
    takeFun<String>(::foo)
    takeFun<String>(::<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>fooInt<!>)

    callFun<String, Wrapper<String>>(::createWrapper)
    callFun<Int, Wrapper<Number>>(::createWrapper)
    callFun<String, Wrapper<*>>(::createWrapper)
    callFun<String, Wrapper<Int>>(::<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>createWrapper<!>)

    callFun<Int, Wrapper<Int>>(::createWrapper).baz(::foo)
}
