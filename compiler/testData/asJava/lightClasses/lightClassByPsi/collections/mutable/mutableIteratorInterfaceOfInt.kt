// WITH_STDLIB
package test

interface IMutableIterator : MutableIterator<Int>

abstract class CIterator : IMutableIterator

abstract class CIterator2(d: IMutableIterator) : IMutableIterator by d

open class CIterator3 : IMutableIterator {
    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Int {
        TODO("Not yet implemented")
    }
}