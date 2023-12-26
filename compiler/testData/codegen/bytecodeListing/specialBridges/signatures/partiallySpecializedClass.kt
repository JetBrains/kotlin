// WITH_SIGNATURES
// Test expectations differ between JVM and JVM IR backends, because of KT-40277. This should be revisited once KT-40277 is resolved.

// JVM_ABI_K1_K2_DIFF: KT-63828
class StringMap<V> : MutableMap<String, V> by HashMap<String, V>()

abstract class AbstractStringMap<V> : MutableMap<String, V> by HashMap<String, V>()
