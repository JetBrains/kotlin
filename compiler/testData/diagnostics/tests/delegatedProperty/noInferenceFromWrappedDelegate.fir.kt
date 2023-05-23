// FIR_DUMP
// WITH_REFLECT

import kotlin.reflect.KProperty

// Definitions
class State<T>(var value: T)
<!NOTHING_TO_INLINE!>inline<!> operator fun <T> State<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value
inline fun <T> remember(block: () -> T): T = block()

// list should have a type of List<Int>, not Any?
val list by remember { State(listOf(0)) }
val first = list.<!UNRESOLVED_REFERENCE!>first<!>()
