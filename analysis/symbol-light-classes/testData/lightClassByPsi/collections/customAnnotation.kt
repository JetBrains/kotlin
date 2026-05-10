// WITH_STDLIB
// FULL_JDK
package test

annotation class Ann(val x: Int)

abstract class CCollection<Elem> : Collection<Elem> {
    @Ann(1)
    override fun containsAll(elements: Collection<Elem>): Boolean {
        TODO("Not yet implemented")
    }

    @Ann(2)
    fun foo() {
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: CCollection.class[add;addAll;clear;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray]
