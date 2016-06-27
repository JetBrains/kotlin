package test

import dependency.*

class Outer<E, F> {
    inner class Inner<G> {
        typealias TA<H> = Map<Map<E, F>, Map<G, H>>
    }
}

annotation class Ann
class TypeAliases {

    typealias B = (A) -> Unit

    fun foo(a: A, b: B, ta: Outer<String, Double>.Inner<Int>.TA<Boolean>) {
        b.invoke(a)
    }

    // TODO: annotations are unsupported yet
    @Ann
    private typealias Parametrized<E, F> = Map<E, F>
}
