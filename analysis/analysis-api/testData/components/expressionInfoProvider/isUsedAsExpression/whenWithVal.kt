fun test(v: Any?) {
    when (<expr>val h = v.hashCode()</expr>) {
        is Number -> 5
        else -> 9
    }
}