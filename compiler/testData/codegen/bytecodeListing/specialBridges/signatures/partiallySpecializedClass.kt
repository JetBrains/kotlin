// IGNORE_BACKEND: JVM_IR
// WITH_SIGNATURES

class StringMap<V> : MutableMap<String, V> by HashMap<String, V>()

abstract class AbstractStringMap<V> : MutableMap<String, V> by HashMap<String, V>()
