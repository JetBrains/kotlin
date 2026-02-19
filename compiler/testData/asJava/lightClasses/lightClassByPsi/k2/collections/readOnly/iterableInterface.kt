// WITH_STDLIB
package test

interface IIterable<Elem> : Iterable<Elem>

abstract class CIterable<Elem> : IIterable<Elem>

abstract class CIterable2<Elem>(d: IIterable<Elem>) : IIterable<Elem> by d

open class CIterable3<Elem> : IIterable<Elem> {
    override fun iterator(): Iterator<Elem> {
        TODO("Not yet implemented")
    }
}
// LIGHT_ELEMENTS_NO_DECLARATION: CIterable.class[iterator]