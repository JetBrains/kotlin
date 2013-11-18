fun test(n: Int) {
    var res: String = ""

    <caret>when (n) {
        1 -> println("A")
        2 -> println("B")
        else -> println("C")
    }

    when (n) {
        1 -> res = "one"
        2 -> res = "two"
        else -> {
            res = "unknown"
            return
        }
    }
}