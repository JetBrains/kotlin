// WITH_STDLIB
package test

interface IMutableSet : MutableSet<Int>

abstract class CMutableSet : IMutableSet

abstract class CMutableSet2(d: IMutableSet) : IMutableSet by d

open class CMutableSet3 : IMutableSet {
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

    override fun contains(element: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: CMutableSet.class[contains;contains;getSize;remove;remove;size;toArray;toArray], CMutableSet2.class[size;toArray;toArray], CMutableSet3.class[size;toArray;toArray]