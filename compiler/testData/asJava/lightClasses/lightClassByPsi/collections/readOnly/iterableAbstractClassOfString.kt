// WITH_STDLIB
package test

abstract class SIterable : Iterable<String>

abstract class SIterable2 : Iterable<String> by emptyList<String>()

open class SIterable3 : Iterable<String> {
    override fun iterator(): Iterator<String> {
        TODO("Not yet implemented")
    }
}
