// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// SIBLING:
fun foo(a: Int) {
    val b: Int = 1

    <selection>if (a > 0) throw Exception("")
    if (b + a > 0) return
    println(a - b)</selection>
}