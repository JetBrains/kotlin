// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A

class D {
    val c: Int by IncorrectThis<A>()
}

val cTopLevel: Int by IncorrectThis<A>()

class IncorrectThis<T> {
    fun <R> get(t: Any?, p: KProperty<*>): Int {
        return 1
    }
}
