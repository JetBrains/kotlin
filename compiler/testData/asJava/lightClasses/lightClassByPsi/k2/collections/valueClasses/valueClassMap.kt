// WITH_STDLIB
package test

abstract class CMap : Map<UInt, UInt>

abstract class CMap2 : Map<UInt, UInt> by emptyMap<UInt, UInt>()

open class CMap3 : Map<UInt, UInt> {
    override fun containsKey(key: UInt): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: UInt): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: UInt): UInt? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override val entries: Set<Map.Entry<UInt, UInt>>
        get() = TODO("Not yet implemented")
    override val keys: Set<UInt>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<UInt>
        get() = TODO("Not yet implemented")
}

// LIGHT_ELEMENTS_NO_DECLARATION: CMap.class[clear;compute-QPydM70;computeIfAbsent-mPSJhXU;computeIfPresent-QPydM70;containsKey;containsKey-WZ4Q5Ns;containsValue;containsValue-WZ4Q5Ns;entrySet;get;get-XTOvfJI;getEntries;getKeys;getSize;getValues;keySet;merge-iG8emGs;put-XblXfeM;putAll;putIfAbsent-XblXfeM;remove;remove;remove-gbq4QnA;replace-XblXfeM;replace-zly0blg;replaceAll;size;values], CMap2.class[clear;compute-QPydM70;computeIfAbsent-mPSJhXU;computeIfPresent-QPydM70;containsKey-WZ4Q5Ns;containsValue-WZ4Q5Ns;entrySet;get-XTOvfJI;keySet;merge-iG8emGs;put-XblXfeM;putAll;putIfAbsent-XblXfeM;remove;remove;remove-gbq4QnA;replace-XblXfeM;replace-zly0blg;replaceAll;size;values], CMap3.class[clear;compute-QPydM70;computeIfAbsent-mPSJhXU;computeIfPresent-QPydM70;containsKey-WZ4Q5Ns;containsValue-WZ4Q5Ns;entrySet;get-XTOvfJI;keySet;merge-iG8emGs;put-XblXfeM;putAll;putIfAbsent-XblXfeM;remove;remove;remove-gbq4QnA;replace-XblXfeM;replace-zly0blg;replaceAll;size;values]