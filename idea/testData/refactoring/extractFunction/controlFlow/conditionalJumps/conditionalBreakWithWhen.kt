// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..a) {
        <selection>when {
            a + b > 0 -> break
            a - b > 0 -> break
            else -> println(0)
        }</selection>
    }
    return 1
}