fun test() {
    block("foo") { foo ->
        fun call() {
            consume(foo)
        }

        <caret>call()
    }
}

fun <T> block(obj: T, block: (T) -> Unit) {
    block(obj)
}

fun consume(text: String) {}