class A : Comparable<A> {
    override fun compareTo(other: A): Int = 0
    operator fun rangeTo(other: A): ClosedRange<A> = TODO()
    operator fun rangeUntil(other: A): OpenEndRange<A> = TODO()
}

fun test() {
    val a = A()
    val b = A()
    a..b
    a..<b
}
