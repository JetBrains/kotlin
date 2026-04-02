// WITH_STDLIB
package test

abstract class SMutableMapEntry<VElem> : MutableMap.MutableEntry<String, VElem>

abstract class SMutableMapEntry2<VElem> : MutableMap.MutableEntry<String, VElem> by mutableMapOf<String, VElem>().entries.first()

open class SMutableMapEntry3<VElem> : MutableMap.MutableEntry<String, VElem> {
    override fun setValue(newValue: VElem): VElem {
        TODO("Not yet implemented")
    }

    override val key: String
        get() = TODO("Not yet implemented")
    override val value: VElem
        get() = TODO("Not yet implemented")
}
