// WITH_RUNTIME
// SUGGESTED_NAMES: triple, getT
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var c: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1
    var c: Int = 2

    val t = <selection>if (a > 0) {
        b += a
        c -= b
        b
    }
    else {
        a
    }</selection>
    println(b + c)

    return t
}