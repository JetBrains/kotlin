class Foo : Bar() {
    fun test() {
        <caret>Companion.bar = "baz"
    }

    companion object {
        var bar: String = "bar"
    }
}