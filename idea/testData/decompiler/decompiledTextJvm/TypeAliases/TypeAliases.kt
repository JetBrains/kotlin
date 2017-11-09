package test

import dependency.*
import kotlin.annotation.AnnotationTarget

class Outer<E, F> {
    inner class Inner<G> {
        @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
        typealias TA<H> = Map<Map<E, F>, Map<G, H>>
    }
}

@Target(AnnotationTarget.TYPEALIAS)
annotation class Ann(val value: String)

class TypeAliases {

    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias B = (A) -> Unit

    fun foo(a: A, b: B, ta: Outer<String, Double>.Inner<Int>.TA<Boolean>) {
        b.invoke(a)
    }

    @Ann("OK")
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    private typealias Parametrized<E, F> = Map<E, F>
}
