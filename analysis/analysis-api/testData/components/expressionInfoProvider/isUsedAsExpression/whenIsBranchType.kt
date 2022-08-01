fun test(v: Any?) {
    when (v) {
        is Number -> 5
        is <expr>String</expr> -> 9
        else -> 9
    }
}