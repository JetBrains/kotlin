var a: Any = 1
fun test() {
    if (a is String) {
        <expr>a</expr>.length
    }
}