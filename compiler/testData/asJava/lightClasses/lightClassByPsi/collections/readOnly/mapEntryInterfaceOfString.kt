// WITH_STDLIB
package test

interface IMapEntry : Map.Entry<String, String>

abstract class CMapEntry : IMapEntry

abstract class CMapEntry2(d: IMapEntry) : IMapEntry by d

open class CMapEntry3 : IMapEntry {
    override val key: String
        get() = TODO("Not yet implemented")
    override val value: String
        get() = TODO("Not yet implemented")
}