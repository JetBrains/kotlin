fun foo(x: Any): String {
    return <expr>when (x) {
        is String -> "1"
        else -> "2"
    }</expr>
}