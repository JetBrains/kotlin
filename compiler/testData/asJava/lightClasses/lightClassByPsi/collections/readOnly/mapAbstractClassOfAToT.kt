// WITH_STDLIB
package test

class A

abstract class ATMap<T> : Map<A, T>

abstract class ATMap2<T> : Map<A, T> by emptyMap<A, T>()

open class ATMap3<T> : Map<A, T> {
    override fun containsKey(key: A): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: T): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: A): T? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override val entries: Set<Map.Entry<A, T>>
        get() = TODO("Not yet implemented")
    override val keys: Set<A>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<T>
        get() = TODO("Not yet implemented")
}
