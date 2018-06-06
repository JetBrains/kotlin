// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KCallable

class Foo {
    fun <T> installRoute(handler: T) where T : (String) -> Any?, T : KCallable<*> {
    }

    fun <T> installRoute(handler: T) where T : () -> Any?, T : KCallable<*> {
    }

    fun foo() {
        <!NI;NONE_APPLICABLE, OI;OVERLOAD_RESOLUTION_AMBIGUITY!>installRoute<!><Any>(::<!NI;DEBUG_INFO_MISSING_UNRESOLVED!>route<!>)
    }

}

fun route(s: String): Any? = null