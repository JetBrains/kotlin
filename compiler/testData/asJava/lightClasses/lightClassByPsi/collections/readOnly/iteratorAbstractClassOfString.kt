// WITH_STDLIB
package test

abstract class SIterator : Iterator<String>

abstract class SIterator2 : Iterator<String> by emptyList<String>().iterator()

open class SIterator3 : Iterator<String> {
    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): String {
        TODO("Not yet implemented")
    }
}
