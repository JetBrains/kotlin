fun test(n: Int): String {
    var res: String

    <caret>res = if (n == 1) "one" else "two"

    return res
}