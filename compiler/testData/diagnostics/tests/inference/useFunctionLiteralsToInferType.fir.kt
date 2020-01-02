package m

import java.util.HashSet

// ----------------------------------
fun <K, V> testGetOrPut(result : MutableMap<K, Set<V>>, key: K) {
    result.getOrPut(key) { HashSet() }
}

fun <K,V> MutableMap<K,V>.getOrPut(key: K, defaultValue: ()-> V) : V = throw Exception("$key $defaultValue")

// ----------------------------------
class Property<T: Comparable<T>>(val name: String, val default: () -> T) {}

fun testProperty() = Property("", { -1.toLong() })
fun testProperty1() = Property("", { "" })
