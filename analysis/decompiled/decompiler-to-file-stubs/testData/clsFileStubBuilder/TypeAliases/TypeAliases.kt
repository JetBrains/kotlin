// FIR_IGNORE
// Ignore reason: test runner performs run on different values of flag `useStringTable` (`true/false`), but checks dumps on the same file.
// This value affects type alias expanding that affects dump.
package test

import dependency.*
import kotlin.annotation.AnnotationTarget

class Outer<E, F> {
    inner class Inner<G> {
        @Suppress("TOPLEVEL_TYPEALIASES_ONLY", "UNSUPPORTED_FEATURE", "WRONG_MODIFIER_TARGET")
        inner typealias TA<H> = Map<Map<E, F>, Map<G, H>>
    }
}

@Target(AnnotationTarget.TYPEALIAS)
annotation class Ann

class TypeAliases {

    class OrderB

    @Suppress("TOPLEVEL_TYPEALIASES_ONLY", "UNSUPPORTED_FEATURE")
    typealias B = (A) -> Unit

    fun foo(a: A, b: B, ta: Outer<String, Double>.Inner<Int>.TA<Boolean>) {
        b.invoke(a)
    }

    @Ann
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY", "UNSUPPORTED_FEATURE")
    private typealias Parametrized<E, F> = Map<E, F>

    fun order(path: String) {}
    fun order(body: Z) {}

    class OrderA
}


typealias Z = dependency.SomeClass // dependency.SomeClass is before (lexicography) kotlin.String, but test.Z is after

