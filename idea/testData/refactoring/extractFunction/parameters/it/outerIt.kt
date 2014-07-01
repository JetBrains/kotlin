// PARAM_TYPES: kotlin.Array<kotlin.Int>
// PARAM_DESCRIPTOR: value-parameter val it: kotlin.Array<kotlin.Int> defined in foo.<anonymous>
fun <T> Array<T>.check(f: (T) -> Boolean): Boolean = false

// SIBLING:
fun foo(t: Array<Array<Int>>) {
    if (t.check { <selection>it.check{ it + 1 > 1 }</selection> }) {
        println("OK")
    }
}