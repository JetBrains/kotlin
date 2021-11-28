// FIR_IDENTICAL
// WITH_STDLIB
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T, R> foo(x: kotlin.reflect.KFunction1<T, R>) {}
fun <T, R> foo(x: kotlin.reflect.KProperty1<T, R>) {}

class Sample {
    fun bar() {}
    fun bar(x: Int) {}
}

class A {
    val foo  = "hello"
    fun foo(b: Boolean) = 1
}

fun test() {
    foo(Sample::bar)
    foo(String::toInt)

    foo<A, String>(A::foo)
}
