// WITH_STDLIB
package test

abstract class CMap<KElem, VElem> : Map<KElem, VElem>

abstract class CMap2<KElem, VElem> : Map<KElem, VElem> by emptyMap<KElem, VElem>()

open class CMap3<KElem, VElem> : Map<KElem, VElem> {
    override fun containsKey(key: KElem): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: VElem): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: KElem): VElem? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override val entries: Set<Map.Entry<KElem, VElem>>
        get() = TODO("Not yet implemented")
    override val keys: Set<KElem>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<VElem>
        get() = TODO("Not yet implemented")
}
