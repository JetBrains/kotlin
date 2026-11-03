// STDLIB_JDK8
// FULL_JDK
package test

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

abstract class AnnotatedValueMap : Map<String, @TypeAnn Int>

abstract class NotNullGetMap : Map<String, Int> {
    override fun get(key: String): Int = 0
}

// LIGHT_ELEMENTS_NO_DECLARATION: AnnotatedValueMap.class[clear;compute;computeIfAbsent;computeIfPresent;containsKey;containsKey;containsValue;containsValue;entrySet;get;get;getEntries;getKeys;getSize;getValues;keySet;merge;put;putAll;putIfAbsent;remove;remove;replace;replace;replaceAll;size;values], NotNullGetMap.class[clear;compute;computeIfAbsent;computeIfPresent;containsKey;containsKey;containsValue;containsValue;entrySet;getEntries;getKeys;getSize;getValues;keySet;merge;put;putAll;putIfAbsent;remove;remove;replace;replace;replaceAll;size;values]
