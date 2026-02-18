// WITH_STDLIB

package test

interface IMutableList<Elem> : MutableList<Elem>

abstract class CMutableList<Elem> : IMutableList<Elem>

abstract class CMutableList2<Elem>(d: IMutableList<Elem>) : IMutableList<Elem> by d

open class CMutableList3<Elem> : IMutableList<Elem> {
    override fun add(element: Elem): Boolean {
        TODO("Not yet implemented")
    }

    override fun add(index: Int, element: Elem) {
        TODO("Not yet implemented")
    }

    override fun addAll(index: Int, elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun listIterator(): MutableListIterator<Elem> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<Elem> {
        TODO("Not yet implemented")
    }

    override fun remove(element: Elem): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAt(index: Int): Elem {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: Elem): Elem {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Elem> {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")

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

    override fun lastIndexOf(element: Elem): Int {
        TODO("Not yet implemented")
    }

    override fun iterator(): MutableIterator<Elem> {
        TODO("Not yet implemented")
    }
}
// LIGHT_ELEMENTS_NO_DECLARATION: CMutableList.class[getSize;remove;removeAt;size;toArray;toArray], CMutableList2.class[remove;size;toArray;toArray], CMutableList3.class[remove;size;toArray;toArray]