// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM
// See KT-15971

interface Foo {
    fun foo(a: Double = 1.0): Double
}

interface FooChain: Foo

open class Impl {
    fun foo(a: Double) = a
}

class FooImpl : FooChain, Impl()

fun box(): String  {
    if (FooImpl().foo() != 1.0) return "fail"
    if (FooImpl().foo(2.0) != 2.0) return "fail"
    return "OK"
}