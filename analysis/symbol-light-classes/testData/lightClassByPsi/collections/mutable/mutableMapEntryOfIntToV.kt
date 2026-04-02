// WITH_STDLIB
package test

abstract class SMutableMapEntry<VElem> : MutableMap.MutableEntry<Int, VElem>

abstract class SMutableMapEntry2<VElem> : MutableMap.MutableEntry<Int, VElem> by mutableMapOf<Int, VElem>().entries.first()

open class SMutableMapEntry3<VElem> : MutableMap.MutableEntry<Int, VElem> {
    override fun setValue(newValue: VElem): VElem {
        TODO("Not yet implemented")
    }

    override val key: Int
        get() = TODO("Not yet implemented")
    override val value: VElem
        get() = TODO("Not yet implemented")
}
