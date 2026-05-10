fun foo() {
    var x: String? = null

    fun local() {
        x = "bar"
    }

    x = "foo"
    <expr>consume(x)</expr>
}

fun consume(text: String) {}