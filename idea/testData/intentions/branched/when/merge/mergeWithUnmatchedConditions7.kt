// IS_APPLICABLE: false
fun test(n: Int) {
    val res: String

    <caret>when {
        n == 1 -> res = "one"
        n == 2 -> res = "two"
        else -> res = "unknown"
    }

    when {
        n + 1 == 2 -> println("B")
        n + 1 == 3 -> println("A")
        else -> println("C")
    }
}