// WITH_STDLIB
package test

interface IListIterator<Elem> : ListIterator<Elem>

abstract class CListIterator<Elem> : IListIterator<Elem>

abstract class CListIterator2<Elem>(d: IListIterator<Elem>) : IListIterator<Elem> by d

open class CListIterator3<Elem> : IListIterator<Elem> {
    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Elem {
        TODO("Not yet implemented")
    }

    override fun hasPrevious(): Boolean {
        TODO("Not yet implemented")
    }

    override fun previous(): Elem {
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
