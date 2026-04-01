// WITH_STDLIB
package test

abstract class SMutableListIterator : MutableListIterator<Int>

abstract class SMutableListIterator2 : MutableListIterator<Int> by mutableListOf<Int>().listIterator()

open class SMutableListIterator3 : MutableListIterator<Int> {
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
