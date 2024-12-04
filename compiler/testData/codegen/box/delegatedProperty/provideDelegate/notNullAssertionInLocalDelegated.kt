// ISSUE: KT-58013
// WITH_STDLIB
// WITH_REFLECT
// FIR_DUMP
// IGNORE_BACKEND_K1: ANY


import kotlin.reflect.KProperty

data class Ref<D>(val t: D)

class GenericDelegate<G>(val value: G)

operator fun <V> Ref<V>.provideDelegate(a: Any?, p: KProperty<*>): GenericDelegate<V> = GenericDelegate(this.t)

operator fun <W> GenericDelegate<W>.getValue(a: Any?, p: KProperty<*>): W = this.value

fun <E> List<Ref<*>>.getElement(i: Int): Ref<E> = this[i] as Ref<E>

@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
fun test(list: List<Ref<*>>) {
    val data: String by list.getElement(0)!!
    require(data == list[0].t)

    val data2: String by list.getElement(0)
    require(data2 == list[0].t)
}

fun box(): String {
    test(listOf(Ref("q")))
    
    return "OK"
}
