// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int, Number, Comparable<Int>, Any
fun bar(a: Int): Int {
    println(a)
    return a + 10
}

// SIBLING:
fun foo(a: Int) {
    val b: Int = 1

    <selection>when {
        a > 0 -> bar(a)
        else -> b
    }
    </selection>
}
