// WITH_RUNTIME
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var c: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1
    var c: Int = 1
    val pair = 2

    <selection>b += a
    c -= a
    println(b)
    println(c)</selection>

    return b + c
}