context(str: String)
fun foo(block: context(String) Int.() -> Unit) {
    val regularReceiver = 1
    regularReceiver.<expr>block()</expr>
}

// LANGUAGE: +ContextParameters
// IGNORE_STABILITY_K1: candidates