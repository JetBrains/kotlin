fun test(baz: Int) {
    fun irrelevant() {
        val foo = "bar"
    }
    <caret>baz = 42
}