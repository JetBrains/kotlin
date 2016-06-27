package test

import dependency.*

class Outer<E, F> {
    inner class Inner<G> {
        typealias TA<H> = Map<Map<E, F>, Map<G, H>>
    }
}

class TypeAliases {

    typealias B = (A) -> Unit
    fun foo(a: A, b: B, ta: Outer<String, Double>.Inner<Int>.TA<Boolean>) {
        b.invoke(a)
    }
}
