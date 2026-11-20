// STDLIB_JDK8
// FULL_JDK
// LIBRARY_PLATFORMS: JVM
package test

abstract class GenericValueMap<V> : Map<String, V> {
    override fun getOrDefault(key: String, defaultValue: V): V = defaultValue
}

abstract class GenericValueMutableMap<V> : MutableMap<String, V> {
    override fun getOrDefault(key: String, defaultValue: V): V = defaultValue
    override fun remove(key: String, value: V): Boolean = false
}

abstract class GenericKeyMutableMap<K> : MutableMap<K, String> {
    override fun getOrDefault(key: K, defaultValue: String): String = defaultValue
    override fun remove(key: K, value: String): Boolean = false
}

abstract class GenericMutableMap<K, V> : MutableMap<K, V> {
    override fun getOrDefault(key: K, defaultValue: V): V = defaultValue
    override fun remove(key: K, value: V): Boolean = false
}

// LIGHT_ELEMENTS_NO_DECLARATION: GenericKeyMutableMap.class[containsValue;containsValue;entrySet;getEntries;getKeys;getSize;getValues;keySet;size;values], GenericMutableMap.class[entrySet;getEntries;getKeys;getSize;getValues;keySet;size;values], GenericValueMap.class[clear;compute;computeIfAbsent;computeIfPresent;containsKey;containsKey;entrySet;get;get;getEntries;getKeys;getSize;getValues;keySet;merge;put;putAll;putIfAbsent;remove;remove;replace;replace;replaceAll;size;values], GenericValueMutableMap.class[containsKey;containsKey;entrySet;get;get;getEntries;getKeys;getSize;getValues;keySet;remove;remove;size;values]
