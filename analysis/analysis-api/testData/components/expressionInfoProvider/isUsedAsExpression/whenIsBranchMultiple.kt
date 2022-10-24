fun test(v: Any?) {
    when (v) {
        is Number -> 5
        is Boolean, is <expr>String</expr> -> 9
        else -> 9
    }
}