// !LANGUAGE: -TypeInferenceOnGenericsForCallableReferences
// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun <T> bar(s: T) {}
fun <T> complex(t: T, f: (T) -> Unit) {}
fun <T> simple(f: (T) -> Unit) {}

fun test1() {
    complex(1, <!UNSUPPORTED_FEATURE!>::bar<!>)
    simple<String>(<!UNSUPPORTED_FEATURE!>::bar<!>)
}

// ---

fun <T> takeFun(f: (T) -> Unit) {}
fun <T, R> callFun(f: (T) -> R): R = TODO()

fun <T> foo(s: T) {}

open class Wrapper<T>(val value: T)
fun <T, R : Wrapper<in T>> createWrapper(s: T): R = TODO()

fun <T> Wrapper<T>.baz(transform: (T) -> Unit): T = TODO()

fun test2() {
    takeFun<String>(<!UNSUPPORTED_FEATURE!>::foo<!>)

    callFun<String, Wrapper<String>>(<!UNSUPPORTED_FEATURE!>::createWrapper<!>)
    callFun<Int, Wrapper<Number>>(<!UNSUPPORTED_FEATURE!>::createWrapper<!>)
    callFun<String, Wrapper<*>>(<!UNSUPPORTED_FEATURE!>::createWrapper<!>)

    callFun<Int, Wrapper<Int>>(<!UNSUPPORTED_FEATURE!>::createWrapper<!>).baz(<!UNSUPPORTED_FEATURE!>::foo<!>)
}

// ---

fun test3() {
    val a1: Array<Double.(Double) -> Double> = arrayOf(Double::plus, Double::minus)
    val a2: Array<Double.(Int) -> Double> = arrayOf(Double::plus, Double::minus)
}

// ---

class A1 {
    fun <T> a1(t: T): Unit {}
    fun test1(): (String) -> Unit = A1()::a1
}

class A2 {
    fun <K, V> a2(key: K): V = TODO()

    fun test1(): (String) -> Unit = A2()::a2
    fun <T3> test2(): (T3) -> T3 = A2()::a2
}

// ---

fun foo1(x: Int?) {}
fun foo1(y: String?) {}
fun foo1(z: Boolean) {}

fun <T> baz1(element: (T) -> Unit): T? = null

fun test4() {
    val a1: Int? = baz1(::foo1)
    val a2: String? = baz1(::foo1)
}