// STDLIB_JDK8
// FULL_JDK
// LIBRARY_PLATFORMS: JVM
package test

abstract class StringMap : AbstractMap<String, String>()

abstract class IntMap : AbstractMap<Int, Int>()

abstract class GenericMap<K, V> : AbstractMap<K, V>()

abstract class GenericValueMap<V> : AbstractMap<String, V>()

abstract class BelowGenericValueMap : GenericValueMap<String>()

abstract class BelowStringMap : StringMap()

abstract class OverridingMap : AbstractMap<String, String>() {
    override fun getOrDefault(key: String, defaultValue: String): String = defaultValue
}

abstract class StringMutableMap : AbstractMutableMap<String, String>()

abstract class IntMutableMap : AbstractMutableMap<Int, Int>()

abstract class GenericKeyMutableMap<K> : AbstractMutableMap<K, String>()

abstract class DirectMutableMap<K, V> : MutableMap<K, V>

abstract class BelowDirectMutableMap : DirectMutableMap<String, String>()

abstract class DirectStringMutableMap : MutableMap<String, String>

abstract class BelowDirectStringMutableMap : DirectStringMutableMap()

abstract class AbstractGetOrDefaultMap<K> : AbstractMap<K, String>() {
    abstract override fun getOrDefault(key: K, defaultValue: String): String
}

abstract class BelowAbstractGetOrDefaultMap : AbstractGetOrDefaultMap<String>()

object MapObject : AbstractMap<String, String>() {
    override val entries: Set<Map.Entry<String, String>> get() = emptySet()
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: AbstractGetOrDefaultMap.class[getOrDefault]
// LIGHT_ELEMENTS_NO_DECLARATION: AbstractGetOrDefaultMap.class[containsValue;containsValue;getOrDefault], BelowAbstractGetOrDefaultMap.class[containsKey;containsKey;get;get;getOrDefault;getOrDefault], BelowDirectMutableMap.class[containsKey;containsKey;containsValue;containsValue;get;get;getOrDefault;getOrDefault;remove;remove;remove;remove], BelowGenericValueMap.class[containsValue;containsValue;get;get;getOrDefault;getOrDefault], DirectMutableMap.class[entrySet;getEntries;getKeys;getSize;getValues;keySet;size;values], DirectStringMutableMap.class[containsKey;containsKey;containsValue;containsValue;entrySet;get;get;getEntries;getKeys;getSize;getValues;keySet;remove;remove;size;values], GenericKeyMutableMap.class[containsValue;containsValue;remove;remove], GenericValueMap.class[containsKey;containsKey;get;get;getOrDefault;getOrDefault], IntMap.class[containsKey;containsKey;containsValue;containsValue;get;get;getOrDefault;getOrDefault], IntMutableMap.class[containsKey;containsKey;containsValue;containsValue;get;get;getOrDefault;getOrDefault;remove;remove;remove;remove], MapObject.class[containsKey;containsKey;containsValue;containsValue;get;get;getOrDefault;getOrDefault], OverridingMap.class[containsKey;containsKey;containsValue;containsValue;get;get], StringMap.class[containsKey;containsKey;containsValue;containsValue;get;get;getOrDefault;getOrDefault], StringMutableMap.class[containsKey;containsKey;containsValue;containsValue;get;get;getOrDefault;getOrDefault;remove;remove;remove;remove]
