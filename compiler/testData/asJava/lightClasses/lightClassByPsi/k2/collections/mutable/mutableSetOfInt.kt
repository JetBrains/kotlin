// WITH_STDLIB
package test

abstract class SMutableSet : MutableSet<Int>

abstract class SMutableSet2 : MutableSet<Int> by mutableSetOf<Int>()

open class SMutableSet3 : MutableSet<Int> {
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

// LIGHT_ELEMENTS_NO_DECLARATION: SMutableSet.class[contains;contains;getSize;remove;remove;size;toArray;toArray], SMutableSet2.class[size;toArray;toArray], SMutableSet3.class[size;toArray;toArray]