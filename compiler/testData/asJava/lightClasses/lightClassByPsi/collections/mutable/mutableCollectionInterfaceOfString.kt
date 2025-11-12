// WITH_STDLIB
package test

interface IMutableCollection : MutableCollection<String>

abstract class CCollection : IMutableCollection

abstract class CCollection2(d: IMutableCollection) : IMutableCollection by d

open class CCollection3 : IMutableCollection {
    override fun add(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun iterator(): MutableIterator<String> {
        TODO("Not yet implemented")
    }

    override fun remove(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun contains(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }
}