// WITH_STDLIB
package test

abstract class SMutableListIterator : MutableListIterator<String>

abstract class SMutableListIterator2 : MutableListIterator<String> by mutableListOf<String>().listIterator()

open class SMutableListIterator3 : MutableListIterator<String> {
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
