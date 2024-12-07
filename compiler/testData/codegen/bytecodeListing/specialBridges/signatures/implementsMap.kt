// WITH_SIGNATURES

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
