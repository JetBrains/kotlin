// FIR_IDENTICAL
// ISSUE: KT-65341
class Controller<K> {
    fun yield(k: K) {}
}

fun <T> generate(lambda: Controller<T>.() -> Unit) {}

fun <E> id(e: E): E = e

// Regular function irrelevant to PCLA
fun bar(s: String) {}

fun foo() {
    generate {
        // `bar(..)` itself is a regular function that should not be postponed
        // But its argument is a string interpolation that contains a postponed call `id(this)`
        // And by our current rule, the whole call needs to be postponed, too
        bar("${id(this)}")

        yield("")
    }
}
