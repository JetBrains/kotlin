// ISSUE: KT-58013
// WITH_REFLECT
// FIR_DUMP

import kotlin.reflect.KProperty

data class Ref<D>(val t: D)

class GenericDelegate<G>(val value: G)

operator fun <V> Ref<V>.provideDelegate(a: Any?, p: KProperty<*>): GenericDelegate<V> = GenericDelegate(this.t)

operator fun <W> GenericDelegate<W>.getValue(a: Any?, p: KProperty<*>): W = this.value

fun <E> List<Ref<*>>.getElement(i: Int): Ref<E> = this[i] <!UNCHECKED_CAST!>as Ref<E><!>

fun test(list: List<Ref<*>>) {
    val data: String by list.getElement(0)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>

    val data2: String by list.getElement(0)
}
