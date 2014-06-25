// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    <selection>if (a + b > 0) return 1
    else if (a - b < 0) return 2
    else return b</selection>
}