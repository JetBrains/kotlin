// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers

context(Int, String)
fun foo(): String {
    return ""
}

context(Int)
fun foo(): Int {
    return 42
}

context(String)
fun foo(): Double {
    return 42.0
}

fun test() {
    with(42) {
        with("") {
            val a: String = foo()
        }
        val b: Int = foo()
    }
    with("") {
        val c: Double = foo()
    }
}
