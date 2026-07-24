// STDLIB_JDK8
// FULL_JDK
package test

interface IMutableIterable : MutableIterable<String>

abstract class CIterable : IMutableIterable

abstract class CIterable2(d: IMutableIterable) : IMutableIterable by d

open class CIterable3 : IMutableIterable {
    override fun iterator(): MutableIterator<String> {
        TODO("Not yet implemented")
    }
}
