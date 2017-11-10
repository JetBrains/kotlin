// FLOW: IN

fun test(n: Int): String {
    var s = when {
        n > 0 -> "+"
        n < 0 -> "-"
        else -> null
    }
    return <caret>s
}