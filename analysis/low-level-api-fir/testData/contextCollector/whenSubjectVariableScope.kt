fun test(a: Any) {
    when (val x = a) {
        is String -> {}
    }
    <expr>a</expr>
}
