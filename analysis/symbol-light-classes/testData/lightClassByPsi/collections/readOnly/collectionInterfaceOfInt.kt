// WITH_STDLIB
package test

interface ICollection : Collection<Int>

abstract class CCollection : ICollection

abstract class CCollection2(d: ICollection) : ICollection by d

open class CCollection3 : ICollection {
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun contains(element: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<Int> {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: CCollection.class[add;addAll;clear;contains;contains;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], CCollection2.class[add;addAll;clear;remove;removeAll;removeIf;retainAll;size;toArray;toArray], CCollection3.class[add;addAll;clear;remove;removeAll;removeIf;retainAll;size;toArray;toArray]