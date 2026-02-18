// WITH_STDLIB
package test

interface IList : List<Int>

abstract class CList : IList

abstract class CList2(d: IList) : IList by d

open class CList3 : IList {
    override val size: Int
        get() = TODO("Not yet implemented")

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
}

// LIGHT_ELEMENTS_NO_DECLARATION: CList.class[add;add;addAll;addAll;clear;contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;listIterator;listIterator;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;subList;toArray;toArray], CList2.class[add;add;addAll;addAll;clear;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;toArray;toArray], CList3.class[add;add;addAll;addAll;clear;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;toArray;toArray]