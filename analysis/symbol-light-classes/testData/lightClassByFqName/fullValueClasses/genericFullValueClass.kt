// pack.Pair
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

value class Pair<A, B : Any>(val first: A, val second: B?) {
    fun <C> map(transform: (A) -> C): Pair<C, B> = Pair(transform(first), second)
}
