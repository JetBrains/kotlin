// WITH_STDLIB
package test

interface IMap<KElem, VElem> : Map<KElem, VElem>

abstract class CMap<KElem, VElem> : IMap<KElem, VElem>

abstract class CMap2<KElem, VElem>(d: IMap<KElem, VElem>) : IMap<KElem, VElem> by d

open class CMap3<KElem, VElem> : IMap<KElem, VElem> {
    override val entries: Set<Map.Entry<KElem, VElem>>
        get() = TODO("Not yet implemented")
    override val keys: Set<KElem>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<VElem>
        get() = TODO("Not yet implemented")

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
}
