// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: val b: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..a) {
        <selection>when {
            a + b > 0 -> break
            a - b > 0 -> break
            else -> {
                println(0)
                break
            }
        }</selection>
    }
    return 1
}