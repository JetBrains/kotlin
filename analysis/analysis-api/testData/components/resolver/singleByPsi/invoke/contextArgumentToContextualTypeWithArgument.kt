context(str: String)
fun foo(block: context(String) (Int) -> Unit) {
    val regularArgument = 1
    <expr>block(regularArgument)</expr>
}

// LANGUAGE: +ContextParameters