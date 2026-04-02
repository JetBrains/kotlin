// WITH_STDLIB
package test

abstract class SIterator : Iterator<Int>

abstract class SIterator2 : Iterator<Int> by emptyList<Int>().iterator()

open class SIterator3 : Iterator<Int> {
    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Int {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: SIterator.class[remove], SIterator2.class[remove], SIterator3.class[remove]