// WITH_STDLIB
package test

abstract class CSet<Elem> : Set<Elem>

abstract class CSet2<Elem> : Set<Elem> by emptySet<Elem>()

open class CSet3<Elem> : Set<Elem> {
    override fun contains(element: Elem): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<Elem> {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")
}

// LIGHT_ELEMENTS_NO_DECLARATION: CSet.class[add;addAll;clear;getSize;iterator;remove;removeAll;retainAll;size;toArray;toArray], CSet2.class[add;addAll;clear;remove;removeAll;retainAll;size;toArray;toArray], CSet3.class[add;addAll;clear;remove;removeAll;retainAll;size;toArray;toArray]