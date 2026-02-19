context(str: String)
fun Int.foo(block: context(String) Int.() -> Unit) {
    <expr>block()</expr>
}

// LANGUAGE: +ContextParameters
// IGNORE_STABILITY_K1: candidates