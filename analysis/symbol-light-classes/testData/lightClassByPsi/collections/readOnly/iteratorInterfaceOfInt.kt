// WITH_STDLIB
package test

interface IIterator : Iterator<Int>

abstract class CIterator : IIterator

abstract class CIterator2(d: IIterator) : IIterator by d

open class CIterator3 : IIterator {
    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Int {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: CIterator.class[remove], CIterator2.class[remove], CIterator3.class[remove]