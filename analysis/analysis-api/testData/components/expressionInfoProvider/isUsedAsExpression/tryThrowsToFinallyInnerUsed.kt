fun test() {
    val x = try {
        throw Exception()
    } finally {
        <expr>9</expr>
    }
}