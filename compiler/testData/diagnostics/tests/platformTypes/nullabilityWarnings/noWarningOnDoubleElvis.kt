// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun test() {
    take(nullable() ?: nullable() ?: "foo")
}

fun <T> nullable(): T? = TODO()
fun take(x: Any) {}