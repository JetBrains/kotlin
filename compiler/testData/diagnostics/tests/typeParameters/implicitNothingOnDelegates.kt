// !DIAGNOSTICS: -UNCHECKED_CAST
// !WITH_NEW_INFERENCE
// WITH_RUNTIME
// WITH_REFLECT
// SKIP_TXT

import kotlin.reflect.*

private object Scope {
    class Inv<T>

    class Delegate<T>(private val p: Inv<in T>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return materialize()
        }
    }

    fun <T> materialize(): T = Any() as T

    fun test(i: Inv<out Number>) {
        val <!UNUSED_VARIABLE!>p<!>: Int by <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>Scope.Delegate(i)<!>
    }
}
