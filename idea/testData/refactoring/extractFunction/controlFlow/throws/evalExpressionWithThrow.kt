// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1

    val t = <selection>if (a > 0) throw Exception("") else a + b</selection>
    return t
}