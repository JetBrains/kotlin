// FIR_IDENTICAL
// ISSUE: KT-58013
// WITH_REFLECT
// FIR_DUMP

import kotlin.reflect.KProperty

data class Ref<D>(val t: D)

operator fun <V> Ref<V>.getValue(hisRef: Any?, property: KProperty<*>): V = this.t

fun <E> List<Ref<*>>.getElement(i: Int): Ref<E> = this[i] <!UNCHECKED_CAST!>as Ref<E><!>

fun test(list: List<Ref<*>>) {
    val data: String by list.getElement(0)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
}
