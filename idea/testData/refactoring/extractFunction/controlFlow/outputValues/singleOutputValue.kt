// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1

    <selection>b += a
    println(b)</selection>

    return b
}