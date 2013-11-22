// IS_APPLICABLE: false
fun test(n: Int) {
    val res: String

    <caret>when (n) {
        1 -> res = "one"
        2 -> res = "two"
        else -> res = "unknown"
    }

    when (n) {
        1 -> println("A")
        2 -> println("B")
        3 -> println("D")
        else -> println("C")
    }
}