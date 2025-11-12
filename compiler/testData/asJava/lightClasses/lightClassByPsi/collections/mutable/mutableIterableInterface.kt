// WITH_STDLIB
package test

interface IMutableIterable<Elem> : MutableIterable<Elem>

abstract class CIterable<Elem> : IMutableIterable<Elem>

abstract class CIterable2<Elem>(d: IMutableIterable<Elem>) : IMutableIterable<Elem> by d

open class CIterable3<Elem> : IMutableIterable<Elem> {
    override fun iterator(): MutableIterator<Elem> {
        TODO("Not yet implemented")
    }
}