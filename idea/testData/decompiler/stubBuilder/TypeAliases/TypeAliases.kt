package test

import dependency.*
import kotlin.annotation.AnnotationTarget

class Outer<E, F> {
    inner class Inner<G> {
        typealias TA<H> = Map<Map<E, F>, Map<G, H>>
    }
}

@Target(AnnotationTarget.TYPEALIAS)
annotation class Ann

class TypeAliases {

    typealias B = (A) -> Unit

    fun foo(a: A, b: B, ta: Outer<String, Double>.Inner<Int>.TA<Boolean>) {
        b.invoke(a)
    }

    @Ann
    private typealias Parametrized<E, F> = Map<E, F>
}
