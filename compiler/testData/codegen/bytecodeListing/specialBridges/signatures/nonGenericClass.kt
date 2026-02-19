// WITH_SIGNATURES
// LANGUAGE: +JvmEnhancedBridges

class StringStringMap : MutableMap<String, String> by HashMap<String, String>()

abstract class AbstractStringStringMap : MutableMap<String, String> by HashMap<String, String>()
