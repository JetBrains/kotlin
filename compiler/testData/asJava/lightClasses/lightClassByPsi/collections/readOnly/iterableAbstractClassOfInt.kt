// WITH_STDLIB
package test

abstract class SIterable : Iterable<Int>

abstract class SIterable2 : Iterable<Int> by emptyList<Int>()

open class SIterable3 : Iterable<Int> {
    override fun iterator(): Iterator<Int> {
        TODO("Not yet implemented")
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: SIterable.class[iterator]