// ISSUE: KT-91111
// ISSUE: KT-91509

// MODULE: lib
// FILE: lib.kt
open class Base<T>(val value: T) {
    fun foo(): T = value
    val bar: T get() = value
}

// The functions below are intentionally never called: once they are inlined, building fake overrides for the copied classes
// fails in the IR linker (KT-91509), see genericLocalClassChainFakeOverrideInInlineFun.kt.
// Without a call site, the test still checks that `AvoidLocalFOsInInlineFunctionsLowering` handles calls to fake overrides
// through a chain of generic local classes: it must neither fail nor leak type parameters of the local classes
// into the dispatch receiver cast. The exact type of the cast is checked by the IR dump: it must be `Base<R of test*>`.

// A chain of generic local classes capturing the enclosing inline function's TP, so the supertype of `L2` is `L1<T, R>`.
inline fun <R> test1(r: R): R = object {
    fun run(): R {
        open class L1<T>(v: T) : Base<T>(v)
        class L2<T>(v: T) : L1<T>(v)
        L2(r).bar
        return L2(r).foo()
    }
}.run()

// Local class TPs mapped to the supertype in a non-trivial way, with type arguments different from the captured TP.
inline fun <R> test2(r: R): R = object {
    fun run(): R {
        open class L1<T>(v: T) : Base<T>(v)
        class L2<A, B>(val a: A, b: B) : L1<B>(b)
        L2(1, r).bar
        return L2(1, r).foo()
    }
}.run()

// An anonymous object extending a generic local class.
inline fun <R> test3(r: R): R = object {
    fun run(): R {
        open class L1<T>(v: T) : Base<T>(v)
        val o = object : L1<R>(r) {}
        o.bar
        return o.foo()
    }
}.run()

// MODULE: main(lib)
// FILE: main.kt
// TODO: invoke `test*()` from `box()` after KT-91509 is fixed
fun box(): String = "OK"
