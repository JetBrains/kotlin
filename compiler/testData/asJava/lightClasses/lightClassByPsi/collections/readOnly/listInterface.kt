// WITH_STDLIB
package test

interface IList<Elem> : List<Elem>

abstract class CList<Elem> : IList<Elem>

abstract class CList2<Elem>(d: IList<Elem>) : IList<Elem> by d

open class CList3<Elem> : IList<Elem> {
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun contains(element: Elem): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): Elem {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: Elem): Int {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<Elem> {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: Elem): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): ListIterator<Elem> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<Elem> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Elem> {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: CList.class[add;add;addAll;addAll;clear;getSize;listIterator;listIterator;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;subList;toArray;toArray], CList2.class[add;add;addAll;addAll;clear;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;toArray;toArray], CList3.class[add;add;addAll;addAll;clear;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;toArray;toArray]