// WITH_STDLIB
package test

interface IMutableListIterator : MutableListIterator<String>

abstract class CListIterator : IMutableListIterator

abstract class CListIterator2(d: IMutableListIterator) : IMutableListIterator by d

open class CListIterator3 : IMutableListIterator {
    override fun add(element: String) {
        TODO("Not yet implemented")
    }

    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasPrevious(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): String {
        TODO("Not yet implemented")
    }

    override fun nextIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun previous(): String {
        TODO("Not yet implemented")
    }

    override fun previousIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun set(element: String) {
        TODO("Not yet implemented")
    }
}
