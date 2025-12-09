// WITH_STDLIB
package test

abstract class SMutableIterable : MutableIterable<Int>

abstract class SMutableIterable2 : MutableIterable<Int> by mutableListOf<Int>()

open class SMutableIterable3 : MutableIterable<Int> {
    override fun iterator(): MutableIterator<Int> {
        TODO("Not yet implemented")
    }
}