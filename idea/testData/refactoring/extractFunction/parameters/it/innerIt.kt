// PARAM_TYPES: kotlin.Int
fun <T> Array<T>.check(f: (T) -> Boolean): Boolean = false

// SIBLING:
fun foo(t: Array<Array<Int>>) {
    if (t.check { it.check{ <selection>it + 1</selection> > 1 } }) {
        println("OK")
    }
}