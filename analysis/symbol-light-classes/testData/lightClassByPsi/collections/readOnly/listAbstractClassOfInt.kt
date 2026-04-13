// WITH_STDLIB
package test

abstract class SList : List<Int>

abstract class SList2 : List<Int> by emptyList<Int>()

open class SList3 : List<Int> {
    override fun contains(element: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): Int {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: Int): Int {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<Int> {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: Int): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): ListIterator<Int> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<Int> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Int> {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")
}

// LIGHT_ELEMENTS_NO_DECLARATION: SList.class[add;add;addAll;addAll;clear;contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;listIterator;listIterator;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;subList;toArray;toArray], SList2.class[add;add;addAll;addAll;clear;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;toArray;toArray], SList3.class[add;add;addAll;addAll;clear;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;toArray;toArray]