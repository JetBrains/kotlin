// !LANGUAGE: -AdaptedCallableReferenceAgainstReflectiveType +DisableCompatibilityModeForNewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KFunction1

fun foo(x: Int): Unit {} // (1)
fun bar(f: KFunction1<Int, Unit>) {}

fun test() {
    bar(::foo) // OK, foo resolved to (1)
}

object Scope {
    fun foo(x: Int, y: Int = 0): Int = 0 // (2)

    fun test() {
        bar(::foo) // Error and foo should be resolved to (2)
    }
}

object Local {
    fun baz(x: Int, y: Int = 0): Int = 0

    fun test() {
        bar(::<!UNRESOLVED_REFERENCE!>baz<!>)
    }
}
