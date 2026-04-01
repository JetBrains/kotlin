context(str: String)
fun foo(block: context(String) Int.(Boolean) -> Unit) {
    val regularParameter = true
    val regularReceiver = 1
    <expr>block(regularReceiver, regularParameter)</expr>
}

// LANGUAGE: +ContextParameters