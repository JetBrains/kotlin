class Test {
    var foo: Int = 1
        <caret>set(value) {
            bar(field)
        }

    fun bar(i: Int) {}
}