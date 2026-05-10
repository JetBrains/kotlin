// WITH_STDLIB
// FULL_JDK
package test

interface IIterable : Iterable<Int>

abstract class CIterable : IIterable

abstract class CIterable2(d: IIterable) : IIterable by d

open class CIterable3 : IIterable {
    override fun iterator(): Iterator<Int> {
        TODO("Not yet implemented")
    }
}
// LIGHT_ELEMENTS_NO_DECLARATION: CIterable.class[iterator]