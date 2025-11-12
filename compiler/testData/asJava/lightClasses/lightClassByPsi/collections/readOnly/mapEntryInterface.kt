// WITH_STDLIB
package test

interface IMapEntry<KElem, VElem> : Map.Entry<KElem, VElem>

abstract class CMapEntry<KElem, VElem> : IMapEntry<KElem, VElem>

abstract class CMapEntry2<KElem, VElem>(d: IMapEntry<KElem, VElem>) : IMapEntry<KElem, VElem> by d

open class CMapEntry3<KElem, VElem> : IMapEntry<KElem, VElem> {
    override val key: KElem
        get() = TODO("Not yet implemented")
    override val value: VElem
        get() = TODO("Not yet implemented")
}