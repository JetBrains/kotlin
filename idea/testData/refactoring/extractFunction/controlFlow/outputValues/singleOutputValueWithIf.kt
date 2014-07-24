// PARAM_TYPES: kotlin.Int, Comparable<Int>
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1

    <selection>if (a > 0) {
        b = b + 1
    }
    println(b)</selection>

    return b
}