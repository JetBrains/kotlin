fun foo(s: String) {}

data class Example(private val str: String) {
    fun doWithExample(block : (Example) -> Unit) = block(Example("hello"))

    fun runExample() {
        doWithExample { example<caret> ->
            foo(example.str)
        }
    }
}