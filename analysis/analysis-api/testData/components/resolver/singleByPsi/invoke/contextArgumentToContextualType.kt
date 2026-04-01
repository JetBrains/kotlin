context(str: String)
fun foo(block: context(String) () -> Unit) {
    <expr>block()</expr>
}

// LANGUAGE: +ContextParameters