// WITH_STDLIB
package test

abstract class CList : List<UInt>

abstract class CList2 : List<UInt> by emptyList<UInt>()

open class CList3 : List<UInt> {
    override fun contains(element: UInt): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<UInt>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): UInt {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: UInt): Int {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<UInt> {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: UInt): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): ListIterator<UInt> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<UInt> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<UInt> {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")
}

// LIGHT_ELEMENTS_NO_DECLARATION: CList.class[add-Qn1smSk;add-WZ4Q5Ns;addAll;addAll;clear;contains;contains-WZ4Q5Ns;getSize;indexOf;indexOf-WZ4Q5Ns;lastIndexOf;lastIndexOf-WZ4Q5Ns;listIterator;listIterator;remove;remove-OGnWXxg;removeAll;replaceAll;retainAll;set-8fN1j4Y;size;sort;subList;toArray;toArray], CList2.class[add-Qn1smSk;add-WZ4Q5Ns;addAll;addAll;clear;contains-WZ4Q5Ns;get-OGnWXxg;indexOf-WZ4Q5Ns;lastIndexOf-WZ4Q5Ns;remove;remove-OGnWXxg;removeAll;replaceAll;retainAll;set-8fN1j4Y;size;sort;toArray;toArray], CList3.class[add-Qn1smSk;add-WZ4Q5Ns;addAll;addAll;clear;contains-WZ4Q5Ns;get-OGnWXxg;indexOf-WZ4Q5Ns;lastIndexOf-WZ4Q5Ns;remove;remove-OGnWXxg;removeAll;replaceAll;retainAll;set-8fN1j4Y;size;sort;toArray;toArray]
// DECLARATIONS_NO_LIGHT_ELEMENTS: CList2.class[get], CList3.class[get]