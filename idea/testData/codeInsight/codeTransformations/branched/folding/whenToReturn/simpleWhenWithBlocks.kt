fun test(n: Int): String {
    <caret>when(n) {
        1 -> {
            println("***")
            return "one"
        }
        else -> {
            println("***")
            return "two"
        }
    }
}