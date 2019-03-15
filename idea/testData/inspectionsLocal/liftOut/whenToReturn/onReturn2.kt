// HIGHLIGHT: INFORMATION
fun test(n: Int): String {
    when (n) {
        1 -> return "one"
        else -> <caret>return "two"
    }
}