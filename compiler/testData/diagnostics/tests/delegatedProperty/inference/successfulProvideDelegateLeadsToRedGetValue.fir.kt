// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-61077

val test: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> materializeDelegate()

fun <T: CharSequence> materializeDelegate(): Box<T> = TODO()

operator fun <K: Comparable<K>> Box<K>.provideDelegate(receiver: Any?, property: kotlin.reflect.KProperty<*>): K = TODO()

operator fun <Q: Comparable<Q>> Q.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Q = TODO()

class Box<V> {}
