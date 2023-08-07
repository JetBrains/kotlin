fun test() {
    val x = 0

    fun call() {
        consume(x)
    }

    <caret>call()
}

fun consume(n: Int) {}