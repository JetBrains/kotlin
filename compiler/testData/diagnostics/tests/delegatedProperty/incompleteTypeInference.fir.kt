// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A

class D {
    val c: Int by <!UNRESOLVED_REFERENCE!>IncorrectThis<!><A>()
}

val cTopLevel: Int by <!UNRESOLVED_REFERENCE!>IncorrectThis<!><A>()

class IncorrectThis<T> {
    fun <R> get(t: Any?, p: KProperty<*>): Int {
        return 1
    }
}
