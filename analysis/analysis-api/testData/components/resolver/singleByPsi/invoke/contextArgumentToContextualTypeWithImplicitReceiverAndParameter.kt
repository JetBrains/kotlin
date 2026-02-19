context(str: String)
fun Int.foo(block: context(String) Int.(Boolean) -> Unit) {
    val regularParameter = true
    <expr>block(regularParameter)</expr>
}

// LANGUAGE: +ContextParameters
// IGNORE_STABILITY_K1: candidates