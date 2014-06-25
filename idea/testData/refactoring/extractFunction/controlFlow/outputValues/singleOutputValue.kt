// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1

    <selection>b += a
    println(b)</selection>

    return b
}