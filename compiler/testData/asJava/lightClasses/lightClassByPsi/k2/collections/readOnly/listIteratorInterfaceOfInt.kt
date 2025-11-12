// WITH_STDLIB
package test

interface IListIterator : ListIterator<Int>

abstract class CListIterator : IListIterator

abstract class CListIterator2(d: IListIterator) : IListIterator by d

open class CListIterator3 : IListIterator {
    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Int {
        TODO("Not yet implemented")
    }

    override fun hasPrevious(): Boolean {
        TODO("Not yet implemented")
    }

    override fun previous(): Int {
        TODO("Not yet implemented")
    }

    override fun nextIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun previousIndex(): Int {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: CListIterator.class[add;hasNext;next;remove;set], CListIterator2.class[add;remove;set], CListIterator3.class[add;remove;set]
