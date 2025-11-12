// WITH_STDLIB
package test

abstract class SCollection : Collection<String>

abstract class SCollection2 : Collection<String> by emptyList<String>()

open class SCollection3 : Collection<String> {
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

    override val size: Int
        get() = TODO("Not yet implemented")
}
