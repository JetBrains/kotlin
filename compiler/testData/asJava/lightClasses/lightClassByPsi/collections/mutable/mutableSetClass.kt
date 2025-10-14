// WITH_STDLIB
package test

abstract class CMutableSet<Elem> : MutableSet<Elem>

abstract class CMutableSet2<Elem> : MutableSet<Elem> by mutableSetOf<Elem>()

open class CMutableSet3<Elem> : MutableSet<Elem> {
    override fun add(element: Elem): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun iterator(): MutableIterator<Elem> {
        TODO("Not yet implemented")
    }

    override fun remove(element: Elem): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun contains(element: Elem): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }
}
