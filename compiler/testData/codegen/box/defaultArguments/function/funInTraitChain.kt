// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BRIDGE_ISSUES
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