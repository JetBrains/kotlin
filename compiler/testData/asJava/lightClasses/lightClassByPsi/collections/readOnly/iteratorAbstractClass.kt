// WITH_STDLIB
package test

abstract class CIterator<Elem> : Iterator<Elem>

abstract class CIterator2<Elem> : Iterator<Elem> by emptyList<Elem>().iterator()

open class CIterator3<Elem> : Iterator<Elem> {
    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Elem {
        TODO("Not yet implemented")
    }
}
