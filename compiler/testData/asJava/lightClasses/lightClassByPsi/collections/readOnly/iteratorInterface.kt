// WITH_STDLIB
package test

interface IIterator<Elem> : Iterator<Elem>

abstract class CIterator<Elem> : IIterator<Elem>

abstract class CIterator2<Elem>(d: IIterator<Elem>) : IIterator<Elem> by d

open class CIterator3<Elem> : IIterator<Elem> {
    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): Elem {
        TODO("Not yet implemented")
    }
}
