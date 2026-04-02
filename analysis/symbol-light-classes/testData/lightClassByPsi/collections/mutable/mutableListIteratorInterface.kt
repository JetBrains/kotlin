// WITH_STDLIB
package test

interface IMutableListIterator<Elem> : MutableListIterator<Elem>

abstract class CListIterator<Elem> : IMutableListIterator<Elem>

abstract class CListIterator2<Elem>(d: IMutableListIterator<Elem>) : IMutableListIterator<Elem> by d

open class CListIterator3<Elem> : IMutableListIterator<Elem> {
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
