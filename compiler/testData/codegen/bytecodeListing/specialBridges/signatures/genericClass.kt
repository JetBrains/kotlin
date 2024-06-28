// JVM_ABI_K1_K2_DIFF: KT-63828
// WITH_SIGNATURES

class GenericMap<K, V> : MutableMap<K, V> by HashMap<K, V>()

abstract class AbstractGenericMap<K, V> : MutableMap<K, V> by HashMap<K, V>()
