// WITH_STDLIB
package test

abstract class SCollection : Collection<Int>

abstract class SCollection2 : Collection<Int> by emptyList<Int>()

open class SCollection3 : Collection<Int> {
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

    override val size: Int
        get() = TODO("Not yet implemented")
}

// LIGHT_ELEMENTS_NO_DECLARATION: SCollection.class[add;addAll;clear;contains;contains;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], SCollection2.class[add;addAll;clear;remove;removeAll;removeIf;retainAll;size;toArray;toArray], SCollection3.class[add;addAll;clear;remove;removeAll;removeIf;retainAll;size;toArray;toArray]