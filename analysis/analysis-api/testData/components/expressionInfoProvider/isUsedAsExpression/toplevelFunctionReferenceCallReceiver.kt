fun callee(): Int {
    return 598
}

fun test() {
    <expr>(::callee)</expr>() + 45
}