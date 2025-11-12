// WITH_STDLIB
package test

interface IIterable : Iterable<String>

abstract class CIterable : IIterable

abstract class CIterable2(d: IIterable) : IIterable by d

open class CIterable3 : IIterable {
    override fun iterator(): Iterator<String> {
        TODO("Not yet implemented")
    }
}
// LIGHT_ELEMENTS_NO_DECLARATION: CIterable.class[iterator]