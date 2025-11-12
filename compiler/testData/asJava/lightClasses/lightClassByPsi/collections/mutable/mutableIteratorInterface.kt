// WITH_STDLIB
package test

interface IMutableIterator<Elem> : MutableIterator<Elem>

abstract class CIterator<Elem> : IMutableIterator<Elem>

abstract class CIterator2<Elem>(d: IMutableIterator<Elem>) : IMutableIterator<Elem> by d

open class CIterator3<Elem> : IMutableIterator<Elem> {
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