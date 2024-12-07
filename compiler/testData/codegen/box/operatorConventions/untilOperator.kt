// WITH_STDLIB
// LANGUAGE: +RangeUntilOperator

class ARange(_start: A, _end: A): ClosedRange<A>, Iterable<A> {
    override val endInclusive: A = _end
    override val start: A = _start
    override fun iterator(): Iterator<A> = object : Iterator<A> {
        override fun hasNext(): Boolean = hasNext
        override fun next(): A {
            val value = next
            if (value == finalElement) {
                if (!hasNext) throw kotlin.NoSuchElementException()
                hasNext = false
            }
            else {
                next += step
            }
            return A(value)
        }
    }

    private val step: Int = 1
    private val finalElement: Int = endInclusive.x
    private var hasNext: Boolean = if (step > 0) start <= endInclusive else start >= endInclusive
    private var next: Int = if (hasNext) start.x else finalElement
}

class A(val x: Int): Comparable<A> {
    operator fun rangeUntil(other: A): Iterable<A> = ARange(this, A(other.x - 1))
    operator fun rangeTo(other: A): Iterable<A> = ARange(this, other)
    override fun compareTo(other: A): Int = this.x.compareTo(other.x)
}

fun box(): String {
    val x = A(1)
    val y = A(4)

    var summ1 = 0
    for (i in x..<y) {
        summ1 += i.x
    }
    var summ2 = 0
    for (i in x..y) {
        summ2 += i.x
    }
    return if (summ1 == 6 && summ2 == 10) "OK" else "NOK"
}
