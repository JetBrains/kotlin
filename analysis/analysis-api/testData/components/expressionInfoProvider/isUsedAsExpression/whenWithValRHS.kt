fun test(v: Any?) {
    when (val h = <expr>v.hashCode()</expr>) {
        is Number -> 5
        else -> 9
    }
}