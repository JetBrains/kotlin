class A(val n: Int) {
    fun compareTo(other: A): Int = n.compareTo(other.n)
}

fun test() {
    A(0) compareTo A(1)
    A(0) < A(1)
    A(0) <= A(1)
    A(0) > A(1)
    A(0) >= A(1)
}