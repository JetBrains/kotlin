// WITH_STDLIB
package test

abstract class SMutableList : MutableList<Int>

abstract class SMutableList2 : MutableList<Int> by mutableListOf<Int>()

open class SMutableList3 : MutableList<Int> {
    override fun add(element: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun add(index: Int, element: Int) {
        TODO("Not yet implemented")
    }

    override fun addAll(index: Int, elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun listIterator(): MutableListIterator<Int> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<Int> {
        TODO("Not yet implemented")
    }

    override fun remove(element: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAt(index: Int): Int {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: Int): Int {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Int> {
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

    override fun iterator(): MutableIterator<Int> {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): Int {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: Int): Int {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: Int): Int {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: SMutableList.class[contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;remove;remove;remove;removeAt;size;toArray;toArray], SMutableList2.class[size;toArray;toArray], SMutableList3.class[size;toArray;toArray]