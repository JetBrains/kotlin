// WITH_STDLIB
package test

interface IMutableIterable : MutableIterable<Int>

abstract class CIterable : IMutableIterable

abstract class CIterable2(d: IMutableIterable) : IMutableIterable by d

open class CIterable3 : IMutableIterable {
    override fun iterator(): MutableIterator<Int> {
        TODO("Not yet implemented")
    }
}