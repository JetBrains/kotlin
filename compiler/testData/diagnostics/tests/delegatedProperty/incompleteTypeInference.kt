// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A

class D {
    val c: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>IncorrectThis<A>()<!>
}

val cTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>IncorrectThis<A>()<!>

class IncorrectThis<T> {
    fun <R> get(t: Any?, p: KProperty<*>): Int {
        return 1
    }
}
