// WITH_STDLIB
package test

abstract class SCollection : Collection<UInt>

abstract class SCollection2 : Collection<UInt> by emptyList<UInt>()

open class SCollection3 : Collection<UInt> {
    override fun contains(element: UInt): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<UInt>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<UInt> {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")
}

// LIGHT_ELEMENTS_NO_DECLARATION: SCollection.class[add-WZ4Q5Ns;addAll;clear;contains;contains-WZ4Q5Ns;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], SCollection2.class[add-WZ4Q5Ns;addAll;clear;contains-WZ4Q5Ns;remove;removeAll;removeIf;retainAll;size;toArray;toArray], SCollection3.class[add-WZ4Q5Ns;addAll;clear;contains-WZ4Q5Ns;remove;removeAll;removeIf;retainAll;size;toArray;toArray]