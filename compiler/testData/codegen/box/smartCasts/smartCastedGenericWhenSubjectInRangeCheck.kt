// ISSUE: KT-68446

fun <T> test_1(arg: T): String {
    if (arg is Char) {
        return when (arg) {
            in 'A'..'Z' -> "O"
            else -> "Not in range"
        }
    }
    return "Not char"
}

fun <T> test_2(arg: T): String {
    if (arg is Char) {
        return when (val s = arg) {
            in 'A'..'Z' -> "K"
            else -> "Not in range"
        }
    }
    return "Not char"
}

fun box(): String {
    val c = 'B'
    return test_1(c) + test_2(c)
}
