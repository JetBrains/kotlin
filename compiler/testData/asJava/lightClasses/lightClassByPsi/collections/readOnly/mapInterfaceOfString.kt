// WITH_STDLIB
package test

interface IMap : Map<String, String>

abstract class CMap : IMap

abstract class CMap2(d: IMap) : IMap by d

open class CMap3 : IMap {
    override val entries: Set<Map.Entry<String, String>>
        get() = TODO("Not yet implemented")
    override val keys: Set<String>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<String>
        get() = TODO("Not yet implemented")

    override fun containsKey(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: String): String? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }
}
