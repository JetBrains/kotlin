fun test(n: Int): String {
    <caret>return when(n) {
    1 -> {
        println("***")
        "one"
    }
    else -> {
        println("***")
        "two"
    }
}