// WITH_SIGNATURES
// LANGUAGE: +JvmEnhancedBridges

class StringMap<V> : MutableMap<String, V> by HashMap<String, V>()

abstract class AbstractStringMap<V> : MutableMap<String, V> by HashMap<String, V>()
