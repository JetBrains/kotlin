// KotlinClass

abstract class KotlinClass<K : RegularInterface, V: RegularInterface?> : Map<K, V>

interface RegularInterface

// LIGHT_ELEMENTS_NO_DECLARATION: KotlinClass.class[clear;compute;computeIfAbsent;computeIfPresent;containsKey;containsKey;containsValue;containsValue;entrySet;get;get;getEntries;getKeys;getSize;getValues;keySet;merge;put;putAll;putIfAbsent;remove;remove;replace;replace;replaceAll;size;values]
