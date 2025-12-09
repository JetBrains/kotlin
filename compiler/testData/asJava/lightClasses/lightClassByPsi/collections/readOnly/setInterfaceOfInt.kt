// WITH_STDLIB
package test

interface ISet : Set<Int>

abstract class CSet : ISet

abstract class CSet2(d: ISet) : ISet by d

open class CSet3 : ISet {
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

// LIGHT_ELEMENTS_NO_DECLARATION: CSet.class[add;addAll;clear;contains;contains;getSize;iterator;remove;removeAll;retainAll;size;toArray;toArray], CSet2.class[add;addAll;clear;remove;removeAll;retainAll;size;toArray;toArray], CSet3.class[add;addAll;clear;remove;removeAll;retainAll;size;toArray;toArray]