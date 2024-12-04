// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A

class D {
    val c: Int <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> IncorrectThis<A>()
}

val cTopLevel: Int <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> IncorrectThis<A>()

class IncorrectThis<T> {
    fun <R> get(t: Any?, p: KProperty<*>): Int {
        return 1
    }
}
