// ISSUE: KT-58874
// WITH_STDLIB

import kotlin.reflect.KProperty

class State<S>(var value: S)
operator fun <V> State<V>.getValue(thisRef: Any?, property: KProperty<*>) = value
inline fun <M> remember(block: () -> M): M = block()

// list should have a type of List<Int>, not Any?
val list0 by remember { State(listOf(1)) }

fun expectInt(i: Int) {}

fun box(): String {
    val list1 by remember { State(listOf(1)) }
    val l0 = list0[0]
    expectInt(l0)
    val l1 = list1[0]
    expectInt(l1)
    if (l0 == 1 && l1 == 1) return "OK" else return "$l0$l1"
}
