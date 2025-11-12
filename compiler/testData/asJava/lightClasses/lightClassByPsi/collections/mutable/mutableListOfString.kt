// WITH_STDLIB
package test

abstract class SMutableList : MutableList<String>

abstract class SMutableList2 : MutableList<String> by mutableListOf<String>()

open class SMutableList3 : MutableList<String> {
    override fun add(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun add(index: Int, element: String) {
        TODO("Not yet implemented")
    }

    override fun addAll(index: Int, elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun listIterator(): MutableListIterator<String> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<String> {
        TODO("Not yet implemented")
    }

    override fun remove(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAt(index: Int): String {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: String): String {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<String> {
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

    override fun iterator(): MutableIterator<String> {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): String {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: String): Int {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: String): Int {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: SMutableList.class[contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;remove;remove;remove;removeAt;size;toArray;toArray], SMutableList2.class[remove;size;toArray;toArray], SMutableList3.class[remove;size;toArray;toArray]