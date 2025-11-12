// WITH_STDLIB
package test

abstract class CIterable<Elem> : Iterable<Elem>

abstract class CIterable2<Elem> : Iterable<Elem> by emptyList()

open class CIterable3<Elem> : Iterable<Elem> {
    override fun iterator(): Iterator<Elem> {
        TODO("Not yet implemented")
    }
}
