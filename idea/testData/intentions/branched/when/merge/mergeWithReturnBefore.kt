// IS_APPLICABLE: false
fun test(n: Int) {
    var res: String = ""

    <caret>when (n) {
        1 -> res = "one"
        2 -> res = "two"
        else -> {
            res = "unknown"
            return
        }
    }

    when (n) {
        1 -> println("A")
        2 -> println("B")
        else -> println("C")
    }
}