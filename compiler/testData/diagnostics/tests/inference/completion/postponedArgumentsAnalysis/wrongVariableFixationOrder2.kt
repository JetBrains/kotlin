// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// Issue: KT-39633

interface Proxy<in D>

class A<E : Any>(val left: E) : Proxy<E>

abstract class Api {
    abstract fun <T> magic(): T
    inline fun <reified A : Any> match(proxy: Proxy<A>): A = magic()
    inline fun <reified B : Any> f(x: B): B = g(x)
    inline fun <reified C : Any> g(x: C) = match(A(x))
}