// WITH_STDLIB
package test

interface IMutableIterator : MutableIterator<String>

abstract class CIterator : IMutableIterator

abstract class CIterator2(d: IMutableIterator) : IMutableIterator by d

open class CIterator3 : IMutableIterator {
    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): String {
        TODO("Not yet implemented")
    }
}