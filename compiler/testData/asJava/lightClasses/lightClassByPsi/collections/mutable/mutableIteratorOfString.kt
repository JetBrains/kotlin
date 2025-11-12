// WITH_STDLIB
package test

abstract class SMutableIterator : MutableIterator<String>

abstract class SMutableIterator2 : MutableIterator<String> by mutableListOf<String>().iterator()

open class SMutableIterator3 : MutableIterator<String> {
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
