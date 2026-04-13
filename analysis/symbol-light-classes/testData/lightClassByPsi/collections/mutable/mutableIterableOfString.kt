// WITH_STDLIB
package test

abstract class SMutableIterable : MutableIterable<String>

abstract class SMutableIterable2 : MutableIterable<String> by mutableListOf<String>()

open class SMutableIterable3 : MutableIterable<String> {
    override fun iterator(): MutableIterator<String> {
        TODO("Not yet implemented")
    }
}