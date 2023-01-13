// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

context(T)
fun <T> useContext(block: (T) -> Unit) { }

fun test() {
    with(42) {
        useContext { i: Int -> i.toDouble() }
    }
}
