fun test(n: Int): String {
    var res: String

    <caret>res = when(n) {
        1 -> "one"
        else -> "two"
    }

    return res
}