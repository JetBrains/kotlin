// WITH_STDLIB
package test

abstract class SListIterator : ListIterator<Int>

abstract class SListIterator2 : ListIterator<Int> by emptyList<Int>().listIterator()

open class SListIterator3 : ListIterator<Int> {
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

// LIGHT_ELEMENTS_NO_DECLARATION: SListIterator.class[add;hasNext;next;remove;set], SListIterator2.class[add;remove;set], SListIterator3.class[add;remove;set]
