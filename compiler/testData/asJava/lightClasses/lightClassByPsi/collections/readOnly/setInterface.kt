// WITH_STDLIB
package test

interface ISet<Elem> : Set<Elem>

abstract class CSet<Elem> : ISet<Elem>

abstract class CSet2<Elem>(d: ISet<Elem>) : ISet<Elem> by d

open class CSet3<Elem> : ISet<Elem> {
    override val size: Int
        get() = TODO("Not yet implemented")

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
}
