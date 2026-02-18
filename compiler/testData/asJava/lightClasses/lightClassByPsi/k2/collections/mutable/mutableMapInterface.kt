// WITH_STDLIB
package test

interface IMutableMap<KElem, VElem> : MutableMap<KElem, VElem>

abstract class CMutableMap<KElem, VElem> : IMutableMap<KElem, VElem>

abstract class CMutableMap2<KElem, VElem>(d: IMutableMap<KElem, VElem>) : IMutableMap<KElem, VElem> by d

open class CMutableMap3<KElem, VElem> : IMutableMap<KElem, VElem> {
    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun put(key: KElem, value: VElem): VElem? {
        TODO("Not yet implemented")
    }

    override fun putAll(from: Map<out KElem, VElem>) {
        TODO("Not yet implemented")
    }

    override fun remove(key: KElem): VElem? {
        TODO("Not yet implemented")
    }

    override val entries: MutableSet<MutableMap.MutableEntry<KElem, VElem>>
        get() = TODO("Not yet implemented")
    override val keys: MutableSet<KElem>
        get() = TODO("Not yet implemented")
    override val values: MutableCollection<VElem>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsKey(key: KElem): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: VElem): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: KElem): VElem? {
        TODO("Not yet implemented")
    }
}
// LIGHT_ELEMENTS_NO_DECLARATION: CMutableMap.class[entrySet;getEntries;getKeys;getSize;getValues;keySet;size;values], CMutableMap2.class[entrySet;keySet;size;values], CMutableMap3.class[entrySet;keySet;size;values]