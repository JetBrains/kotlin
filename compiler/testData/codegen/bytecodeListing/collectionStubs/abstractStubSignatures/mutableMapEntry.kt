// WITH_SIGNATURES

abstract class GenericMutableMutableMapEntry<K, V> : MutableMap.MutableEntry<K, V>

abstract class GenericStringMutableMapEntry<K> : MutableMap.MutableEntry<K, String>

abstract class StringGenericMutableMapEntry<V> : MutableMap.MutableEntry<String, V>

abstract class ByteShortMutableMapEntry : MutableMap.MutableEntry<Byte, Short>

abstract class NumberStringMutableMapEntry : MutableMap.MutableEntry<Number, String>