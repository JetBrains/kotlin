// WITH_STDLIB
// FULL_JDK
package test

abstract class CMutableIterable<Elem> : MutableIterable<Elem>

abstract class CMutableIterable2<Elem> : MutableIterable<Elem> by mutableListOf<Elem>()

open class CMutableIterable3<Elem> : MutableIterable<Elem> {
    override fun iterator(): MutableIterator<Elem> {
        TODO("Not yet implemented")
    }
}