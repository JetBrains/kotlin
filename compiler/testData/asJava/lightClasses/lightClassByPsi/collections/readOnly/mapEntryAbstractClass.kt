// WITH_STDLIB
package test

abstract class CMapEntry<KElem, VElem> : Map.Entry<KElem, VElem>

abstract class CMapEntry2<KElem, VElem> : Map.Entry<KElem, VElem> by emptyMap<KElem, VElem>().entries.first()

open class CMapEntry3<KElem, VElem> : Map.Entry<KElem, VElem> {
    override val key: KElem
        get() = TODO("Not yet implemented")
    override val value: VElem
        get() = TODO("Not yet implemented")
}