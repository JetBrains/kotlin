// !LANGUAGE: -AdaptedCallableReferenceAgainstReflectiveType -DisableCompatibilityModeForNewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KFunction1

fun foo(x: Int): Unit {} // (1)
fun bar(f: KFunction1<Int, Unit>) {}

fun test() {
    bar(::foo)
}

object Scope {
    fun foo(x: Int, y: Int = 0): Int = 0 // (2)

    fun test() {
        bar(::foo)
        bar(<!ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE!>Scope::foo<!>)
    }
}

object Local {
    fun baz(x: Int, y: Int = 0): Int = 0

    fun test() {
        bar(<!ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE!>::baz<!>)
    }
}

object WrongType {
    fun foo(x: String, y: Int = 0) {} // (3)

    fun test() {
        bar(::foo) // Should resolve to (1) because (3) has wrong type on top of being adapted
    }
}