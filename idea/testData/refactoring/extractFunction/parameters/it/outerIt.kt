// PARAM_TYPES: kotlin.Array<kotlin.Int>
fun <T> Array<T>.check(f: (T) -> Boolean): Boolean = false

// SIBLING:
fun foo(t: Array<Array<Int>>) {
    if (t.check { <selection>it.check{ it + 1 > 1 }</selection> }) {
        println("OK")
    }
}