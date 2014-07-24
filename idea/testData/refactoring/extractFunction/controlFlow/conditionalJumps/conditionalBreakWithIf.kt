// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: val b: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..a) {
        <selection>if (a + b > 0) break
        println(a - b)
        if (a - b > 0) break
        println(a + b)</selection>
    }
    return 1
}