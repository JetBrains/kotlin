// WITH_STDLIB
package test

interface ISet : Set<String>

abstract class CSet : ISet

abstract class CSet2(d: ISet) : ISet by d

open class CSet3 : ISet {
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun contains(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<String> {
        TODO("Not yet implemented")
    }
}
