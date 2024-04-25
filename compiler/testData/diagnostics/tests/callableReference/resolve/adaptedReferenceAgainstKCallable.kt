// FIR_IDENTICAL
// LANGUAGE: -AdaptedCallableReferenceAgainstReflectiveType
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KCallable

fun take(k: KCallable<*>) {}

fun foo(x: Int = 0) {}

fun test() {
    take(::foo)
}