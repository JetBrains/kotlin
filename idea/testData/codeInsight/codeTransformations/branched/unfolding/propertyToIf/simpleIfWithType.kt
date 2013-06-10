fun test(n: Int): String {
    val <caret>res: jet.String = if (n == 1) "one" else "two"

    return res
}