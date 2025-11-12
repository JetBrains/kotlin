// WITH_STDLIB
package test

interface IMutableListIterator : MutableListIterator<Int>

abstract class CListIterator : IMutableListIterator

abstract class CListIterator2(d: IMutableListIterator) : IMutableListIterator by d

open class CListIterator3 : IMutableListIterator {
    override fun add(element: Int) {
        TODO("Not yet implemented")
    }

    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasPrevious(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Int {
        TODO("Not yet implemented")
    }

    override fun nextIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun previous(): Int {
        TODO("Not yet implemented")
    }

    override fun previousIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun set(element: Int) {
        TODO("Not yet implemented")
    }
}
