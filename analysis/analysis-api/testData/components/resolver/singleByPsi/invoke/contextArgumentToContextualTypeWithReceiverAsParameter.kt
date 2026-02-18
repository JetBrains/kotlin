context(str: String)
fun foo(block: context(String) Int.() -> Unit) {
    val regularReceiver = 1
    <expr>block(regularReceiver)</expr>
}

// LANGUAGE: +ContextParameters