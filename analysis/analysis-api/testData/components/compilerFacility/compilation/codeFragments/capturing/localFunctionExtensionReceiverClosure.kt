fun test() {
    block("foo") {
        fun call() {
            consume(this@block)
        }

        <caret>call()
    }
}

fun <T> block(obj: T, block: T.() -> Unit) {
    obj.block()
}

fun consume(text: String) {}