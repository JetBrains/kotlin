// WITH_STDLIB
package test

abstract class CMutableListIterator<Elem> : MutableListIterator<Elem>

abstract class CMutableListIterator2<Elem> : MutableListIterator<Elem> by mutableListOf<Elem>().listIterator()

open class CMutableListIterator3<Elem> : MutableListIterator<Elem> {
    override fun add(element: Elem) {
        TODO("Not yet implemented")
    }

    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasPrevious(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Elem {
        TODO("Not yet implemented")
    }

    override fun nextIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun previous(): Elem {
        TODO("Not yet implemented")
    }

    override fun previousIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun set(element: Elem) {
        TODO("Not yet implemented")
    }
}
