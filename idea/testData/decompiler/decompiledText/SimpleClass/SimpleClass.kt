package test

import dependency.*

abstract class SimpleClass: D<Tr, Int>(), Tr, List<String> {
    fun f() {
    }

    fun g(d: D<String, Tr>): List<D<A, Int>> {
        throw RuntimeException()
    }

    fun Int.f() {
    }

    private fun privateFun() {
    }

    val a: A = A()

    public var b: B = B()

    val Int.g: Int
        get() = this + 2

    fun <T, K, G> complexFun(a: T, b: K, c: G): G {
        throw RuntimeException()
    }
}
