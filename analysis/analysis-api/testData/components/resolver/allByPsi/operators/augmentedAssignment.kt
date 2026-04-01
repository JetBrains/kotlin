class A {
    operator fun plusAssign(other: A) {}
    operator fun minusAssign(other: A) {}
    operator fun timesAssign(other: A) {}
    operator fun divAssign(other: A) {}
    operator fun remAssign(other: A) {}
}

fun test() {
    val a = A()
    val b = A()
    a += b
    a -= b
    a *= b
    a /= b
    a %= b
}
