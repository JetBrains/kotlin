// WITH_SIGNATURES

class GenericMap<K, V> : MutableMap<K, V> by HashMap<K, V>()

abstract class AbstractGenericMap<K, V> : MutableMap<K, V> by HashMap<K, V>()
