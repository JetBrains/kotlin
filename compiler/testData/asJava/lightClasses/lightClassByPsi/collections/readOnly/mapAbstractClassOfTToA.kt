// WITH_STDLIB
package test

class A

abstract class TAMap<T> : Map<T, A>

abstract class TAMap2<T> : Map<T, A> by emptyMap<T, A>()

open class TAMap3<T> : Map<T, A> {
    override fun containsKey(key: T): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: A): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: T): A? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override val entries: Set<Map.Entry<T, A>>
        get() = TODO("Not yet implemented")
    override val keys: Set<T>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<A>
        get() = TODO("Not yet implemented")
}
