package stdlibRange

fun main(args: Array<String>) {
    A().rangeTo(A()).contains(A())
}

class A: Comparable<A> {
    override fun compareTo(other: A) = 0
}

// ADDITIONAL_BREAKPOINT: Ranges.kt:override fun contains(item: T): Boolean {

// EXPRESSION: start <= item
// RESULT: 1: Z