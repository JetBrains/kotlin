fun builder(block: () -> Unit): Boolean {
    block()
    return true
}

fun unitFunction() {
    return
}

fun test(b: Boolean) {
    builder { <expr>unitFunction()</expr> }
}