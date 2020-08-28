// WITH_SIGNATURES
// Test expectations differ between JVM and JVM IR backends, because of KT-40277. This should be revisited once KT-40277 is resolved.

class StringStringMap : MutableMap<String, String> by HashMap<String, String>()

abstract class AbstractStringStringMap : MutableMap<String, String> by HashMap<String, String>()
