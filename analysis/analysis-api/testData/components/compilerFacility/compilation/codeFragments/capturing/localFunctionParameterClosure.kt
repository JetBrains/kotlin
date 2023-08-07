fun test(x: Int) {
    fun call() {
        consume(x)
    }

    <caret>call()
}

fun consume(n: Int) {}