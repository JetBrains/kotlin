fun test() {
    try {
        throw Exception()
    } finally {
        <expr>9</expr>
    }
}