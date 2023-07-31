// FIR_IGNORE
// Ignore reason: FIR does not support nested typealiases (especially inside inner classes)
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
annotation class Ann

class TypeAliases {

    class OrderB

    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias B = (A) -> Unit

    fun foo(a: A, b: B, ta: Outer<String, Double>.Inner<Int>.TA<Boolean>) {
        b.invoke(a)
    }

    @Ann
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    private typealias Parametrized<E, F> = Map<E, F>

    fun order(path: String) {}
    fun order(body: Z) {}

    class OrderA
}


typealias Z = dependency.SomeClass // dependency.SomeClass is before (lexicography) kotlin.String, but test.Z is after

