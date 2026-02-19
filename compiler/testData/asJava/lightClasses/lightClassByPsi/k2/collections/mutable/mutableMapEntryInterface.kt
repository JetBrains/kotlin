// WITH_STDLIB
package test

interface IMutableMapEntry<KElem, VElem> : MutableMap.MutableEntry<KElem, VElem>

abstract class CMutableMapEntry<KElem, VElem> : IMutableMapEntry<KElem, VElem>

abstract class CMutableMapEntry2<KElem, VElem>(d: IMutableMapEntry<KElem, VElem>) : IMutableMapEntry<KElem, VElem> by d

open class CMutableMapEntry3<KElem, VElem> : IMutableMapEntry<KElem, VElem> {
    override fun setValue(newValue: VElem): VElem {
        TODO("Not yet implemented")
    }

    override val key: KElem
        get() = TODO("Not yet implemented")
    override val value: VElem
        get() = TODO("Not yet implemented")
}