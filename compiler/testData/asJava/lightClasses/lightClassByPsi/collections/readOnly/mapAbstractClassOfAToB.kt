// WITH_STDLIB
package test

class A
class B

abstract class ABMap : Map<A, B>

abstract class ABMap2 : Map<A, B> by emptyMap<A, B>()

open class ABMap3 : Map<A, B> {
    override fun containsKey(key: A): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: B): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: A): B? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override val entries: Set<Map.Entry<A, B>>
        get() = TODO("Not yet implemented")
    override val keys: Set<A>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<B>
        get() = TODO("Not yet implemented")
}
