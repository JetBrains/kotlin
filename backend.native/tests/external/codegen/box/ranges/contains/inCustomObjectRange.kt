// WITH_RUNTIME

class A(val z: Int) : Comparable<A> {
    override fun compareTo(other: A): Int {
        return z.compareTo(other.z)
    }
}

operator fun  A.rangeTo(that: A): ClosedRange<A> = object: ClosedRange<A> {
    override val endInclusive: A
        get() = that
    override val start: A
        get() = this@rangeTo
}

operator fun ClosedRange<A>.iterator() = (start.z..endInclusive.z).map { A(it) }.iterator()

fun box(): String {
    if (!( A(2) in A(1)..A(12) )) return "Fail 1"
    if ( A(2) !in A(1)..A(12) ) return "Fail 2"
    return "OK"
}
