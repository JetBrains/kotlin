fun builder(block: () -> Unit): Boolean {
    block()
    return true
}

fun nonunitFunction(): Int {
    return 54
}

fun test(b: Boolean) {
    builder { <expr>nonunitFunction()</expr> }
}