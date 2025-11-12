// WITH_STDLIB
package test

abstract class SSet : Set<String>

abstract class SSet2 : Set<String> by emptySet<String>()

open class SSet3 : Set<String> {
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

// LIGHT_ELEMENTS_NO_DECLARATION: SSet.class[add;addAll;clear;contains;contains;getSize;iterator;remove;removeAll;retainAll;size;toArray;toArray], SSet2.class[add;addAll;clear;remove;removeAll;retainAll;size;toArray;toArray], SSet3.class[add;addAll;clear;remove;removeAll;retainAll;size;toArray;toArray]