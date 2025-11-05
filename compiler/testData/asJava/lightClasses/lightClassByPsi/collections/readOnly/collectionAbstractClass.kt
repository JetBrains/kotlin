// WITH_STDLIB
package test

abstract class CCollection<Elem> : Collection<Elem>

abstract class CCollection2<Elem> : Collection<Elem> by emptyList()

open class CCollection3<Elem> : Collection<Elem> {
    override fun contains(element: Elem): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<Elem> {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")
}