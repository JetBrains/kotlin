// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: val b: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int) {
    val b: Int = 1

    <selection>if (a > 0) throw Exception("")
    if (b + a > 0) return
    println(a - b)</selection>
}