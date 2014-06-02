fun foo(a: Int, b: Int): Int {
    fun bar() {
        return <selection>a + b - 1</selection>
    }

    return bar()
}