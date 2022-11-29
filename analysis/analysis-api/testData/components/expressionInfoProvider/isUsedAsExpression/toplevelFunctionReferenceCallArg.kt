fun callee(x: Int): Int {
    return 598 + x
}

fun test() {
    (::callee)(<expr>657</expr>) + 45
}