// PARAM_TYPES: kotlin.Int, Comparable<Int>
// PARAM_TYPES: kotlin.Int
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1

    <selection>if (a > 0) {
        b = b + 1
    }
    println(b)</selection>

    return b
}