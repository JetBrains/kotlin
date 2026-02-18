// WITH_STDLIB
package test

abstract class CCollection<Elem> : Collection<Elem> {
    override fun contains(element: Elem): Boolean {
        TODO("Not yet implemented")
    }
}

abstract class CCollection2<Elem> : CCollection<Elem>() {
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun containsAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }
}

abstract class CCollection3<Elem> : CCollection2<Elem>() {
    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<Elem> {
        TODO("Not yet implemented")
    }
}
// LIGHT_ELEMENTS_NO_DECLARATION: CCollection.class[add;addAll;clear;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray]