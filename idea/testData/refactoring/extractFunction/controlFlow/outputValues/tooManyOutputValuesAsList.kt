// WITH_RUNTIME
// OPTIONS: true, true, false, false
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var c: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var d: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var e: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1
    var c: Int = 1
    var d: Int = 1
    var e: Int = 1

    <selection>b += a
    c -= a
    d += c
    e -= d
    println(b)
    println(c)</selection>

    return b + c + d + e
}