// WITH_STDLIB

fun test() {
    var numbers = 1..10

    <expr>numbers.forEach {
        if (it == 3) {
            return
        }
    }</expr>

    consume(10)
}

fun consume(n: Int) {}