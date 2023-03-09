// See KT-42033

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57268 K2: extra methods `remove` and/or `getOrDefault` are generated for Map subclasses with JDK 1.6 in dependencies

interface ObservableMap<K, V> : Map<K, V>

abstract class ObservableMutableMap<K, V> : ObservableMap<K, V> {
    fun put(key: K, value: V): V? = value

    fun remove(key: K): V? = null

    fun putAll(from: Map<out K, V>) {
    }
}
