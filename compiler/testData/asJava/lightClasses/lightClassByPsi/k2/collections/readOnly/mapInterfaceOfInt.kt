// WITH_STDLIB
package test

interface IMap : Map<Int, Int>

abstract class CMap : IMap

abstract class CMap2(d: IMap) : IMap by d

open class CMap3 : IMap {
    override val entries: Set<Map.Entry<Int, Int>>
        get() = TODO("Not yet implemented")
    override val keys: Set<Int>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<Int>
        get() = TODO("Not yet implemented")

    override fun containsKey(key: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: Int): Int? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: CMap.class[clear;compute;computeIfAbsent;computeIfPresent;containsKey;containsKey;containsValue;containsValue;entrySet;get;get;getEntries;getKeys;getSize;getValues;keySet;merge;put;putAll;putIfAbsent;remove;remove;replace;replace;replaceAll;size;values], CMap2.class[clear;compute;computeIfAbsent;computeIfPresent;entrySet;keySet;merge;put;putAll;putIfAbsent;remove;remove;replace;replace;replaceAll;size;values], CMap3.class[clear;compute;computeIfAbsent;computeIfPresent;entrySet;keySet;merge;put;putAll;putIfAbsent;remove;remove;replace;replace;replaceAll;size;values]