// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    <selection>println(a)
    if (a > 0) return a</selection>
    return -a
}