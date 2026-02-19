class C<T>(val value: T) {
    operator fun component1(): T = value
    operator fun component2(): T = value
    operator fun component3(): T = value
}

fun test() {
    val (a, b, c) = C(42)
}