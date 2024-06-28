// FIR_IDENTICAL
// ISSUE: KT-58874
// WITH_STDLIB
// FIR_DUMP

import kotlin.reflect.KProperty

class State<S>(var value: S)
operator fun <V> State<V>.getValue(thisRef: Any?, property: KProperty<*>) = value
inline fun <M> remember(block: () -> M): M = block()

// list should have a type of List<Int>, not Any?
val list0 by remember { State(listOf(1)) }

fun expectInt(i: Int) {
    println(i)
}

fun main() {
    val list1 by remember { State(listOf(1)) }
    expectInt(list0[0])
    expectInt(list1[0])
}
