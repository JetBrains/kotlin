// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KCallable

class Foo {
    fun <T> installRoute(handler: T) where T : (String) -> Any?, T : KCallable<*> {
    }

    fun <T> installRoute(handler: T) where T : () -> Any?, T : KCallable<*> {
    }

    fun foo() {
        <!NONE_APPLICABLE!>installRoute<!><Any>(::route)
    }

}

fun route(s: String): Any? = null
