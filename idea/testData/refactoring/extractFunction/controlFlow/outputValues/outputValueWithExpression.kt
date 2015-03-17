// WITH_RUNTIME
// SUGGESTED_NAMES: pair, getT
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1

    val t = <selection>if (a > 0) {
        b += a
        b
    }
    else {
        a
    }</selection>
    println(b)

    return t
}