// WITH_STDLIB

// extensions of `SequenceScope` with the names of its members must not be fused as if they were the members
suspend fun <T> SequenceScope<T>.yieldAll(vararg elements: T) = yieldAll(elements.asIterable())

suspend fun SequenceScope<Int>.yield(a: Int, b: Int) {
    yield(a)
    yield(b)
}

fun box(): String {
    val varargs = sequence { yieldAll(1, 2, 3) }.toList()
    if (varargs != listOf(1, 2, 3)) return "Fail vararg yieldAll: $varargs"
    val pairs = sequence { yield(1, 2); yield(3, 4) }.toList()
    if (pairs != listOf(1, 2, 3, 4)) return "Fail two-argument yield: $pairs"
    return "OK"
}
