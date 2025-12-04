// FIR_IDENTICAL
// WITH_STDLIB

class A {
    @JvmName("bar1") fun <T> bar(t: T) where T : I1, T : I2 = Unit
    @JvmName("bar2") fun <T> bar(t: T) where T : I1, T : I3 = Unit

    @JvmName("foo1") fun <T : I1, I1> foo() = Unit
    @JvmName("foo2") fun <T : I1> foo() = Unit

    fun <T : Number> baz(t: T) = Unit
    fun <T : CharSequence> baz(t: T) = Unit
}

open class Base<R> {
    fun <T : I1> foo(t: T) = Unit
    fun <T : R> foo(t: T) = Unit
}

class B : Base<I1>()

interface I1
interface I2
interface I3
