// PARAM_TYPES: kotlin.Int
fun <T> Array<T>.check(f: (T) -> Boolean): Boolean = false

// SIBLING:
fun foo(t: Array<Int>) {
    if (t.check { <selection>it + 1</selection> > 1 }) {
        println("OK")
    }
}