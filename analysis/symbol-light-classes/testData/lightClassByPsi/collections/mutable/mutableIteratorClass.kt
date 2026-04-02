// WITH_STDLIB
package test

abstract class CMutableIterator<Elem> : MutableIterator<Elem>

abstract class CMutableIterator2<Elem> : MutableIterator<Elem> by mutableListOf<Elem>().iterator()

open class CMutableIterator3<Elem> : MutableIterator<Elem> {
    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Elem {
        TODO("Not yet implemented")
    }
}
