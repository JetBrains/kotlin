// PARAM_TYPES: kotlin.Int
// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    return when (a + b) {
        0 -> <selection>b</selection>
        1 -> -b
        else -> a - b
    }
}
