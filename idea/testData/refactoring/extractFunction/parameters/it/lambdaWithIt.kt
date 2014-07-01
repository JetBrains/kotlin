// PARAM_TYPES: kotlin.Array<kotlin.Int>
// PARAM_DESCRIPTOR: value-parameter val t: kotlin.Array<kotlin.Int> defined in foo
fun <T> Array<T>.check(f: (T) -> Boolean): Boolean = false

// SIBLING:
fun foo(t: Array<Int>) {
    if (<selection>t.check { it + 1 > 1 }</selection>) {
        println("OK")
    }
}