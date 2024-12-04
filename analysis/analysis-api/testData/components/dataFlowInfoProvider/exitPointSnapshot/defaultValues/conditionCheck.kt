fun <T> Array<T>.check(f: (T) -> Boolean): Boolean = false

fun test(words: Array<String>) {
    if (<expr>words.check { it.length == 5 }</expr>) {
        consume("OK")
    }
}

fun consume(text: String) {}