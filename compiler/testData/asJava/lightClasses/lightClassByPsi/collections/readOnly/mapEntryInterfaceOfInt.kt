// WITH_STDLIB
package test

interface IMapEntry : Map.Entry<Int, Int>

abstract class CMapEntry : IMapEntry

abstract class CMapEntry2(d: IMapEntry) : IMapEntry by d

open class CMapEntry3 : IMapEntry {
    override val key: Int
        get() = TODO("Not yet implemented")
    override val value: Int
        get() = TODO("Not yet implemented")
}
// LIGHT_ELEMENTS_NO_DECLARATION: CMapEntry.class[setValue], CMapEntry2.class[setValue], CMapEntry3.class[setValue]