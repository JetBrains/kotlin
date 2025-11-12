// WITH_STDLIB
package test

abstract class CList<Elem> : List<Elem>

abstract class CList2<Elem> : List<Elem> by emptyList<Elem>()

open class CList3<Elem> : List<Elem> {
    override fun contains(element: Elem): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): Elem {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: Elem): Int {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<Elem> {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: Elem): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): ListIterator<Elem> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<Elem> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Elem> {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")
}
