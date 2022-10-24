fun builder(block: () -> Int): Boolean {
    block()
    return true
}

fun intFunction(): Int {
    return 5
}

fun test(b: Boolean) {
    builder { <expr>intFunction()</expr> }
}