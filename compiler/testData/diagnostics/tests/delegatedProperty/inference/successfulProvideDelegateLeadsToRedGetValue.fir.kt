// ISSUE: KT-61077

val test: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>materializeDelegate()<!>

fun <T: CharSequence> materializeDelegate(): Box<T> = TODO()

operator fun <K: Comparable<K>> Box<K>.provideDelegate(receiver: Any?, property: kotlin.reflect.KProperty<*>): K = TODO()

operator fun <Q: Comparable<Q>> Q.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Q = TODO()

class Box<V> {}
