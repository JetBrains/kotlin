// Without platform dependent declarations we should see ordinary generic signatures everywhere.
// WITH_SIGNATURES
interface I

abstract class T1<K, V> : MutableMap<K, V> {
    fun getOrDefault(key: K, value: V): V = value
    fun getOrDefault(key: K, value: Int): Int = value + 1
    fun remove(key: K, value: V): Boolean = false
    fun remove(key: K, value: Int): Boolean = true
}

abstract class T2<V> : MutableMap<String, V> {
    fun getOrDefault(key: String, value: V): V = value
    fun getOrDefault(key: Int?, value: V): V = value
    fun remove(key: String, value: V): Boolean = false
    fun remove(key: Int?, value: V): Boolean = true
}

abstract class T3<K, V : I> : MutableMap<K, V> {
    fun getOrDefault(key: K, value: V): V = value
    fun getOrDefault(key: K, value: Int): I? = null
    fun remove(key: K, value: V): Boolean = false
    fun remove(key: K, value: Int): Boolean = true
}

abstract class T4<K, V> : Map<K, V> {
    fun remove(key: K, value: V): Boolean = false
    fun getOrDefault(key: K, value: V): V = value
    fun getOrDefault(key: K, value: Int): Int = value + 1
    fun remove(key: K, value: Int): Boolean = true
}
