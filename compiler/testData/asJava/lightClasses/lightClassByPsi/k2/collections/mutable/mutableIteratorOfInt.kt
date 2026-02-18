// WITH_STDLIB
package test

abstract class SMutableIterator : MutableIterator<Int>

abstract class SMutableIterator2 : MutableIterator<Int> by mutableListOf<Int>().iterator()

open class SMutableIterator3 : MutableIterator<Int> {
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
