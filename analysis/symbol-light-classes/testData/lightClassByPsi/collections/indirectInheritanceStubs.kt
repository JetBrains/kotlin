// STDLIB_JDK8
// FULL_JDK
package test

interface ICollection<Elem> : Collection<Elem>

abstract class CCollection<Elem> : ICollection<Elem>

abstract class CCollection2<Elem> : CCollection<Elem>()

abstract class Foo

abstract class CCollection3 : Foo(), Collection<String>

abstract class CCollection4 : Foo(), ICollection<String>

abstract class CCollection5 : CCollection4()

object CCollection6 : Collection<String> {
    override fun contains(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<String> {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")
}

interface IIterable : Iterable<String> {
    override fun iterator(): Iterator<String> = TODO()
}

abstract class CIterable : IIterable

interface IList : List<String> {
    override fun subList(fromIndex: Int, toIndex: Int): List<String> = TODO()
}

abstract class CList : IList

interface IListIterator : ListIterator<String> {
    override fun hasNext(): Boolean = false
}

abstract class CListIterator : IListIterator

interface IIterator : Iterator<String> {
    fun remove() {}
}

abstract class CIterator : IIterator

// LIGHT_ELEMENTS_NO_DECLARATION: CCollection.class[add;addAll;clear;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], CCollection3.class[add;addAll;clear;contains;contains;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], CCollection4.class[add;addAll;clear;contains;contains;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], CCollection6.class[add;addAll;clear;remove;removeAll;removeIf;retainAll;size;toArray;toArray], CIterable.class[iterator], CIterator.class[remove], CList.class[add;add;addAll;addAll;clear;contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;listIterator;listIterator;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;subList;toArray;toArray], CListIterator.class[add;hasNext;next;remove;set]
