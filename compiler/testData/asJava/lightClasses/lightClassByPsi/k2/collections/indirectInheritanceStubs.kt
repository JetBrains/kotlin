// WITH_STDLIB
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

// LIGHT_ELEMENTS_NO_DECLARATION: CCollection.class[add;addAll;clear;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], CCollection3.class[add;addAll;clear;contains;contains;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], CCollection4.class[add;addAll;clear;contains;contains;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], CCollection6.class[add;addAll;clear;remove;removeAll;removeIf;retainAll;size;toArray;toArray]