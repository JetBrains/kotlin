// WITH_SIGNATURES

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57268 K2: extra methods `remove` and/or `getOrDefault` are generated for Map subclasses with JDK 1.6 in dependencies

interface MapN<K : Number, V> : Map<K, V>

abstract class MapImpl<A, B> : Map<A, B> {
    override fun containsKey(key: A): Boolean = false
}

abstract class MapSImpl<B> : Map<String, B> {
    override fun containsKey(key: String): Boolean = false
}

abstract class MapNImpl<A : Number, B> : MapN<A, B> {
    override fun containsKey(key: A): Boolean = false
}
