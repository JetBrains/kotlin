// WITH_STDLIB
package test

interface IMutableCollection : MutableCollection<Int>

abstract class CCollection : IMutableCollection

abstract class CCollection2(d: IMutableCollection) : IMutableCollection by d

open class CCollection3 : IMutableCollection {
    override fun add(element: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun iterator(): MutableIterator<Int> {
        TODO("Not yet implemented")
    }

    override fun remove(element: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun contains(element: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }
}
// LIGHT_ELEMENTS_NO_DECLARATION: CCollection.class[contains;contains;getSize;remove;remove;size;toArray;toArray], CCollection2.class[size;toArray;toArray], CCollection3.class[size;toArray;toArray]