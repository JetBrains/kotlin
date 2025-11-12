// WITH_STDLIB
package test

interface IMutableMap : MutableMap<String, String>

abstract class CMutableMap : IMutableMap

abstract class CMutableMap2(d: IMutableMap) : IMutableMap by d

open class CMutableMap3 : IMutableMap {
    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun put(key: String, value: String): String? {
        TODO("Not yet implemented")
    }

    override fun putAll(from: Map<out String, String>) {
        TODO("Not yet implemented")
    }

    override fun remove(key: String): String? {
        TODO("Not yet implemented")
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = TODO("Not yet implemented")
    override val keys: MutableSet<String>
        get() = TODO("Not yet implemented")
    override val values: MutableCollection<String>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsKey(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: String): String? {
        TODO("Not yet implemented")
    }
}