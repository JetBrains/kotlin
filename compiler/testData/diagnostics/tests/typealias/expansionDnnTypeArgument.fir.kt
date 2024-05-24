// ISSUE: KT-68383
interface Inv2<K, V> {
    fun get(k: K): V
}

typealias Inv1<T> = Inv2<String, T & Any>

fun test1(inv: Inv1<String>) {
    expectMap(<!ARGUMENT_TYPE_MISMATCH!>inv<!>)
    val x = inv.get("")
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test2(map: Inv1<String?>) {
    // Well, this K1 behavior doesn't look really correct, but we're not going to change it anymore
    expectMap(<!ARGUMENT_TYPE_MISMATCH!>map<!>)
    val x = map.get("")
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun expectMap(x: Inv2<String, String>) {}
