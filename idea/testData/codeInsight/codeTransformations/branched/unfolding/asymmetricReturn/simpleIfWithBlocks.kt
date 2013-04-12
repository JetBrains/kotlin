fun test(n: Int): String {
    <caret>return if (n == 1) {
        println("***")
        "one"
    } else "two"
}