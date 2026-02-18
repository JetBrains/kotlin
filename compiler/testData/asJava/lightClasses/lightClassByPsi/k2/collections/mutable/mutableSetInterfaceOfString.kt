// WITH_STDLIB
package test

interface IMutableSet : MutableSet<String>

abstract class CMutableSet : IMutableSet

abstract class CMutableSet2(d: IMutableSet) : IMutableSet by d

open class CMutableSet3 : IMutableSet {
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

    override fun contains(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: CMutableSet.class[contains;contains;getSize;remove;remove;size;toArray;toArray], CMutableSet2.class[size;toArray;toArray], CMutableSet3.class[size;toArray;toArray]