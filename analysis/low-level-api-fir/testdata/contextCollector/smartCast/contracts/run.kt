// WITH_STDLIB

fun test() {
    val a: String? = null

    run {
        a = "foo"
    }

    <expr>consume(a)</expr>
}

fun consume(text: String) {}